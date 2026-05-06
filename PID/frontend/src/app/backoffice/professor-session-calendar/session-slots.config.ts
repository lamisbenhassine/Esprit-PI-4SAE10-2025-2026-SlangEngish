export interface SessionSlotOption {
  code: string;
  label: string;
}

export interface SessionDayGroup {
  dayKey: string;
  dayLabel: string;
  slots: SessionSlotOption[];
}

/** Aligné sur le backend SessionSlotRegistry. */
export const SESSION_DAY_GROUPS: SessionDayGroup[] = [
  {
    dayKey: 'TUE',
    dayLabel: 'Mardi',
    slots: [
      { code: 'TUE_17_20', label: '17h – 20h' },
      { code: 'TUE_19_22', label: '19h – 22h' }
    ]
  },
  {
    dayKey: 'WED',
    dayLabel: 'Mercredi',
    slots: [
      { code: 'WED_17_20', label: '17h – 20h' },
      { code: 'WED_19_22', label: '19h – 22h' }
    ]
  },
  {
    dayKey: 'SAT',
    dayLabel: 'Samedi',
    slots: [
      { code: 'SAT_09_12', label: '9h – 12h' },
      { code: 'SAT_14_17', label: '14h – 17h' },
      { code: 'SAT_17_20', label: '17h – 20h' }
    ]
  },
  {
    dayKey: 'SUN',
    dayLabel: 'Dimanche',
    slots: [
      { code: 'SUN_09_12', label: '9h – 12h' },
      { code: 'SUN_14_17', label: '14h – 17h' },
      { code: 'SUN_17_20', label: '17h – 20h' }
    ]
  }
];

export function dayKeyFromSlotCode(code: string): string {
  const i = code.indexOf('_');
  return i > 0 ? code.substring(0, i) : code;
}

/** Indices de jour FullCalendar : dimanche = 0 … samedi = 6 */
const FC_DAY_FROM_DAYKEY: Record<string, number> = {
  SUN: 0,
  TUE: 2,
  WED: 3,
  SAT: 6
};

export interface SlotRecurrence {
  daysOfWeek: number[];
  startTime: string;
  endTime: string;
}

/** Récurrence hebdomadaire pour affichage calendrier (FullCalendar). */
export function slotCodeToRecurrence(code: string): SlotRecurrence | null {
  const dayKey = dayKeyFromSlotCode(code);
  const dow = FC_DAY_FROM_DAYKEY[dayKey];
  if (dow === undefined) {
    return null;
  }
  const parts = code.split('_');
  if (parts.length < 3) {
    return null;
  }
  const startH = parts[1].padStart(2, '0');
  const endH = parts[2].padStart(2, '0');
  return {
    daysOfWeek: [dow],
    startTime: `${startH}:00:00`,
    endTime: `${endH}:00:00`
  };
}
