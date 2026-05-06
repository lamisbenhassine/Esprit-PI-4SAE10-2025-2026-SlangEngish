import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { UserContextService } from '../../services/user-context.service';
import {
  ProfessorAvailabilityService,
  ProfessorAvailabilitySlotPayload
} from './professor-availability.service';
import { finalize } from 'rxjs';
import { CalendarOptions, EventClickArg, EventInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import listPlugin from '@fullcalendar/list';
import frLocale from '@fullcalendar/core/locales/fr';
import {
  SESSION_DAY_GROUPS,
  SessionDayGroup,
  SessionSlotOption,
  dayKeyFromSlotCode,
  slotCodeToRecurrence
} from './session-slots.config';
import {
  ProfessorSessionDescriptionsDialogComponent,
  ProfessorSessionDescriptionsDialogResult,
  SessionDescriptionItem
} from './professor-session-descriptions-dialog.component';

interface LockStorageV1 {
  locked: boolean;
  deadline: number | null;
}

@Component({
  selector: 'app-professor-session-calendar',
  templateUrl: './professor-session-calendar.component.html',
  styleUrls: ['./professor-session-calendar.component.css']
})
export class ProfessorSessionCalendarComponent implements OnInit, OnDestroy {
  readonly dayGroups: SessionDayGroup[] = SESSION_DAY_GROUPS;

  readonly lockGracePeriodMs = 60_000;

  professorUserId = 1;
  selectedByDay = new Map<string, string>();
  /** Texte de plan par code de créneau (ex. SAT_09_12). */
  sessionDescriptions = new Map<string, string>();

  loading = false;
  saving = false;
  initialLoadDone = false;

  isLocked = false;
  lockDeadlineMs: number | null = null;
  countdownProgress = 0;
  countdownLabel = '';

  calendarOptions: CalendarOptions | null = null;

  private persistTimer: ReturnType<typeof setTimeout> | undefined;
  private readonly persistDebounceMs = 350;
  private tickTimer: ReturnType<typeof setInterval> | undefined;

  constructor(
    private readonly availabilityApi: ProfessorAvailabilityService,
    private readonly userContext: UserContextService,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.professorUserId = this.userContext.getCurrentUserId();
    this.initCalendarOptions();
    this.load();
  }

  ngOnDestroy(): void {
    if (this.persistTimer != null) {
      clearTimeout(this.persistTimer);
    }
    this.stopTick();
  }

  private initCalendarOptions(): void {
    this.calendarOptions = {
      plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, listPlugin],
      initialView: 'timeGridWeek',
      locale: frLocale,
      firstDay: 1,
      hiddenDays: [1, 4, 5],
      headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'timeGridWeek,dayGridWeek,listWeek'
      },
      buttonText: {
        today: "Aujourd'hui",
        week: 'Semaine',
        day: 'Jour',
        list: 'Liste'
      },
      allDaySlot: false,
      slotMinTime: '09:00:00',
      slotMaxTime: '23:00:00',
      slotDuration: '00:30:00',
      snapDuration: '00:30:00',
      contentHeight: 560,
      nowIndicator: true,
      events: this.buildCalendarEvents(),
      eventClick: (info) => {
        info.jsEvent.preventDefault();
        this.onCalendarEventClick(info);
      }
    };
  }

  private refreshCalendarEvents(): void {
    if (!this.calendarOptions) {
      return;
    }
    this.calendarOptions = {
      ...this.calendarOptions,
      events: this.buildCalendarEvents()
    };
  }

  private eventTitle(slot: SessionSlotOption): string {
    const raw = (this.sessionDescriptions.get(slot.code) ?? '').trim();
    if (!raw) {
      return slot.label;
    }
    const short = raw.length > 90 ? `${raw.slice(0, 87)}…` : raw;
    return `${slot.label}\n${short}`;
  }

  private buildCalendarEvents(): EventInput[] {
    const events: EventInput[] = [];
    for (const group of this.dayGroups) {
      for (const slot of group.slots) {
        const rec = slotCodeToRecurrence(slot.code);
        if (!rec) {
          continue;
        }
        const selected = this.isSelected(slot.code);
        const blocked = this.isSlotChoiceBlocked(slot);
        events.push({
          id: `slot-${slot.code}`,
          title: this.eventTitle(slot),
          daysOfWeek: rec.daysOfWeek,
          startTime: rec.startTime,
          endTime: rec.endTime,
          extendedProps: { slotCode: slot.code },
          backgroundColor: selected ? '#3f51b5' : blocked ? '#cfd8dc' : '#e8eaf6',
          borderColor: selected ? '#303f9f' : '#b0bec5',
          textColor: selected ? '#ffffff' : '#37474f',
          classNames:
            blocked && !selected
              ? ['psc-fc-slot--blocked']
              : selected
                ? ['psc-fc-slot--selected']
                : []
        });
      }
    }
    return events;
  }

  private onCalendarEventClick(info: EventClickArg): void {
    const code = info.event.extendedProps['slotCode'] as string | undefined;
    if (!code) {
      return;
    }
    const slot = this.findSlotByCode(code);
    if (!slot) {
      return;
    }
    if (this.isSelected(code)) {
      this.openDescriptionsEditorForSlot(code);
      return;
    }
    this.toggleSlot(slot);
  }

  private findSlotByCode(code: string): SessionSlotOption | null {
    for (const g of this.dayGroups) {
      const s = g.slots.find((x) => x.code === code);
      if (s) {
        return s;
      }
    }
    return null;
  }

  openDescriptionsEditor(): void {
    if (!this.hasAnySelection()) {
      this.snackBar.open('Sélectionnez au moins un créneau sur le calendrier.', 'OK', { duration: 4000 });
      return;
    }
    this.openDescriptionsDialog(this.buildDescriptionItems());
  }

  openDescriptionsEditorForSlot(slotCode: string): void {
    if (!this.isSelected(slotCode)) {
      return;
    }
    const items = this.buildDescriptionItems().filter((i) => i.slotCode === slotCode);
    this.openDescriptionsDialog(items);
  }

  private buildDescriptionItems(): SessionDescriptionItem[] {
    const items: SessionDescriptionItem[] = [];
    for (const group of this.dayGroups) {
      const code = this.selectedByDay.get(group.dayKey);
      if (!code) {
        continue;
      }
      const opt = group.slots.find((s) => s.code === code);
      items.push({
        slotCode: code,
        dayLabel: group.dayLabel,
        timeLabel: opt?.label ?? code,
        description: this.sessionDescriptions.get(code) ?? ''
      });
    }
    return items;
  }

  private openDescriptionsDialog(items: SessionDescriptionItem[]): void {
    if (items.length === 0) {
      return;
    }
    this.dialog
      .open(ProfessorSessionDescriptionsDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        data: { items, readOnly: this.isLocked }
      })
      .afterClosed()
      .subscribe((result: ProfessorSessionDescriptionsDialogResult | undefined) => {
        if (result == null) {
          return;
        }
        if (result.removed) {
          const code = result.removed;
          if (!this.isLocked) {
            const day = dayKeyFromSlotCode(code);
            if (this.selectedByDay.get(day) === code) {
              this.selectedByDay.delete(day);
              this.sessionDescriptions.delete(code);
              this.onSelectionChangedForLock();
              this.refreshCalendarEvents();
              this.schedulePersist();
            }
          }
          return;
        }
        if (result.descriptions) {
          for (const code of Object.keys(result.descriptions)) {
            this.sessionDescriptions.set(code, result.descriptions[code]);
          }
          this.refreshCalendarEvents();
          this.schedulePersist();
        }
      });
  }

  private lockKey(): string {
    return `profSessionLock_v1_${this.professorUserId}`;
  }

  private readLockStorage(): LockStorageV1 {
    if (!isPlatformBrowser(this.platformId)) {
      return { locked: false, deadline: null };
    }
    try {
      const raw = localStorage.getItem(this.lockKey());
      if (!raw) {
        return { locked: false, deadline: null };
      }
      const o = JSON.parse(raw) as LockStorageV1;
      return {
        locked: !!o.locked,
        deadline: typeof o.deadline === 'number' ? o.deadline : null
      };
    } catch {
      return { locked: false, deadline: null };
    }
  }

  private writeLockStorage(state: LockStorageV1): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    localStorage.setItem(this.lockKey(), JSON.stringify(state));
  }

  private clearLockStorage(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    localStorage.removeItem(this.lockKey());
  }

  load(): void {
    this.loading = true;
    this.initialLoadDone = false;
    this.stopTick();
    this.availabilityApi.get(this.professorUserId).subscribe({
      next: (dto) => {
        this.selectedByDay.clear();
        this.sessionDescriptions.clear();
        for (const s of dto.slots ?? []) {
          const code = s.slotCode?.trim();
          if (!code) {
            continue;
          }
          const day = dayKeyFromSlotCode(code);
          this.selectedByDay.set(day, code);
          this.sessionDescriptions.set(code, (s.description ?? '').trim());
        }
      },
      error: () => {
        this.snackBar.open('Impossible de charger vos disponibilités.', 'OK', { duration: 4000 });
      },
      complete: () => {
        this.loading = false;
        this.initialLoadDone = true;
        this.syncLockStateAfterDataLoad();
        this.refreshCalendarEvents();
      }
    });
  }

  /** Réconcilie verrou / compte à rebours avec le stockage et le nombre de séances chargées. */
  private syncLockStateAfterDataLoad(): void {
    const n = this.selectedByDay.size;
    const st = this.readLockStorage();

    if (st.locked) {
      this.applyLocked(false);
      return;
    }

    if (n < 3) {
      this.isLocked = false;
      this.lockDeadlineMs = null;
      this.countdownProgress = 0;
      this.countdownLabel = '';
      this.clearLockStorage();
      return;
    }

    const now = Date.now();
    if (st.deadline != null) {
      if (now >= st.deadline) {
        this.applyLocked(true);
        return;
      }
      this.lockDeadlineMs = st.deadline;
      this.isLocked = false;
      this.startTick();
      return;
    }

    this.lockDeadlineMs = now + this.lockGracePeriodMs;
    this.writeLockStorage({ locked: false, deadline: this.lockDeadlineMs });
    this.isLocked = false;
    this.startTick();
  }

  private applyLocked(notify: boolean): void {
    this.isLocked = true;
    this.lockDeadlineMs = null;
    this.countdownProgress = 0;
    this.countdownLabel = '';
    this.stopTick();
    this.writeLockStorage({ locked: true, deadline: null });
    if (notify) {
      this.snackBar.open(
        'Délai écoulé : vos 3 séances sont figées. Utilisez Reset pour tout effacer et recommencer.',
        'OK',
        { duration: 6000 }
      );
    }
    this.refreshCalendarEvents();
  }

  private startTick(): void {
    this.stopTick();
    this.refreshCountdownUi();
    this.tickTimer = setInterval(() => this.refreshCountdownUi(), 250);
  }

  private stopTick(): void {
    if (this.tickTimer != null) {
      clearInterval(this.tickTimer);
      this.tickTimer = undefined;
    }
  }

  private refreshCountdownUi(): void {
    if (this.lockDeadlineMs == null) {
      this.countdownProgress = 0;
      this.countdownLabel = '';
      return;
    }
    const now = Date.now();
    const remaining = Math.max(0, this.lockDeadlineMs - now);
    if (remaining <= 0) {
      this.applyLocked(true);
      return;
    }
    this.countdownProgress = (remaining / this.lockGracePeriodMs) * 100;
    const sec = Math.ceil(remaining / 1000);
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    this.countdownLabel = `${m}:${s.toString().padStart(2, '0')}`;
  }

  private onSelectionChangedForLock(): void {
    if (this.isLocked) {
      return;
    }

    const n = this.selectedByDay.size;
    const st = this.readLockStorage();

    if (n < 3) {
      this.lockDeadlineMs = null;
      this.stopTick();
      this.countdownProgress = 0;
      this.countdownLabel = '';
      if (!st.locked) {
        this.writeLockStorage({ locked: false, deadline: null });
      }
      return;
    }

    const now = Date.now();
    if (st.deadline != null && now < st.deadline) {
      this.lockDeadlineMs = st.deadline;
      this.startTick();
      return;
    }
    if (st.deadline != null && now >= st.deadline) {
      this.applyLocked(true);
      return;
    }

    this.lockDeadlineMs = now + this.lockGracePeriodMs;
    this.writeLockStorage({ locked: false, deadline: this.lockDeadlineMs });
    this.startTick();
  }

  hasThreeSessions(): boolean {
    return this.selectedByDay.size === 3;
  }

  showCountdownBar(): boolean {
    return (
      !this.loading &&
      !this.isLocked &&
      this.hasThreeSessions() &&
      this.lockDeadlineMs != null &&
      Date.now() < this.lockDeadlineMs
    );
  }

  isSelected(code: string): boolean {
    const day = dayKeyFromSlotCode(code);
    return this.selectedByDay.get(day) === code;
  }

  readonly maxSessions = 3;

  toggleSlot(slot: SessionSlotOption): void {
    if (this.isLocked) {
      this.snackBar.open('Les créneaux sont verrouillés. Utilisez Reset pour recommencer.', 'OK', { duration: 4000 });
      return;
    }
    const day = dayKeyFromSlotCode(slot.code);
    const current = this.selectedByDay.get(day);
    if (current === slot.code) {
      this.selectedByDay.delete(day);
      this.sessionDescriptions.delete(slot.code);
      this.onSelectionChangedForLock();
      this.schedulePersist();
      this.refreshCalendarEvents();
      return;
    }
    if (current != null && current !== slot.code) {
      this.sessionDescriptions.delete(current);
    }
    const isNewDay = !this.selectedByDay.has(day);
    if (isNewDay && this.selectedByDay.size >= this.maxSessions) {
      this.snackBar.open(
        `Maximum ${this.maxSessions} séances au total (un créneau par jour au plus). Désélectionnez un jour pour en ajouter un autre.`,
        'OK',
        { duration: 5000 }
      );
      return;
    }
    this.selectedByDay.set(day, slot.code);
    this.onSelectionChangedForLock();
    this.schedulePersist();
    this.refreshCalendarEvents();
  }

  isSlotChoiceBlocked(slot: SessionSlotOption): boolean {
    if (this.isLocked) {
      return true;
    }
    const day = dayKeyFromSlotCode(slot.code);
    if (this.selectedByDay.get(day) === slot.code) {
      return false;
    }
    if (this.selectedByDay.has(day)) {
      return false;
    }
    return this.selectedByDay.size >= this.maxSessions;
  }

  hasAnySelection(): boolean {
    return this.selectedByDay.size > 0;
  }

  resetAll(): void {
    if (!this.initialLoadDone || this.loading) {
      return;
    }
    if (this.persistTimer != null) {
      clearTimeout(this.persistTimer);
      this.persistTimer = undefined;
    }
    this.clearLockStorage();
    this.isLocked = false;
    this.lockDeadlineMs = null;
    this.stopTick();
    this.countdownProgress = 0;
    this.countdownLabel = '';
    this.selectedByDay.clear();
    this.sessionDescriptions.clear();
    this.refreshCalendarEvents();
    this.persistNow();
  }

  private schedulePersist(): void {
    if (!this.initialLoadDone || this.loading) {
      return;
    }
    if (this.persistTimer != null) {
      clearTimeout(this.persistTimer);
    }
    this.persistTimer = setTimeout(() => {
      this.persistTimer = undefined;
      this.persistNow();
    }, this.persistDebounceMs);
  }

  private buildSlotsPayload(): ProfessorAvailabilitySlotPayload[] {
    return Array.from(this.selectedByDay.values())
      .sort()
      .map((code) => ({
        slotCode: code,
        description: this.sessionDescriptions.get(code) ?? ''
      }));
  }

  persistNow(): void {
    const slots = this.buildSlotsPayload();
    this.saving = true;
    this.availabilityApi
      .save(this.professorUserId, slots)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {},
        error: (err) => {
          const msg =
            err?.error?.error ??
            (typeof err?.error === 'string' ? err.error : null) ??
            'Enregistrement impossible. Réessayez.';
          this.snackBar.open(msg, 'OK', { duration: 5000 });
        }
      });
  }
}
