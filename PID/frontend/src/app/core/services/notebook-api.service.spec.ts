import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NOTEBOOK_API_URL } from '../../api.config';
import {
  GrammarResult,
  NotebookApiService,
  NotebookDashboard,
  NotebookGameDetail,
  NotebookGameHintResponse,
  NotebookGameProgressRow,
  NotebookGameSubmitAnswerResponse,
  NotebookGameSummary,
  NotebookNote,
  PronunciationCoachResult,
  SummaryResult
} from './notebook-api.service';

describe('NotebookApiService', () => {
  let service: NotebookApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NotebookApiService,
        { provide: NOTEBOOK_API_URL, useValue: '/api/notebook' }
      ]
    });

    service = TestBed.inject(NotebookApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should list notes with userId param', () => {
    const mock: NotebookNote[] = [
      { id: 1, userId: 7, title: 'T1', content: 'C1' }
    ];

    service.listNotes(7).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].id).toBe(1);
    });

    const req = http.expectOne(r => r.method === 'GET' && r.url === '/api/notebook/notes');
    expect(req.request.params.get('userId')).toBe('7');
    req.flush(mock);
  });

  it('should create note via POST', () => {
    const created: NotebookNote = { id: 10, userId: 7, title: 'New', content: 'Body' };

    service.createNote(7, 'New', 'Body').subscribe((res) => {
      expect(res.id).toBe(10);
      expect(res.title).toBe('New');
    });

    const req = http.expectOne('/api/notebook/notes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 7, title: 'New', content: 'Body' });
    req.flush(created);
  });

  it('should call grammar endpoint', () => {
    const resp: GrammarResult = { correctedText: 'fixed', issuesFixed: 1, source: 'languagetool' };

    service.grammar('hello').subscribe((r) => {
      expect(r.correctedText).toBe('fixed');
      expect(r.issuesFixed).toBe(1);
    });

    const req = http.expectOne('/api/notebook/ai/grammar');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ text: 'hello' });
    req.flush(resp);
  });

  it('should call pronunciation coach endpoint', () => {
    const resp: PronunciationCoachResult = {
      overallSummary: 'ok',
      idealSentence: 'Sample',
      items: [],
      overallTips: [],
      rawCoachText: null
    };

    service.pronunciationCoach('target', 'heard').subscribe((r) => {
      expect(r.overallSummary).toBe('ok');
    });

    const req = http.expectOne('/api/notebook/ai/pronunciation-coach');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ targetText: 'target', heardText: 'heard' });
    req.flush(resp);
  });

  it('should list published games', () => {
    const mock: NotebookGameSummary[] = [
      {
        id: 1,
        title: 'G1',
        description: null,
        type: 'CROSSWORD',
        published: true,
        entryCount: 3,
        teacherId: 9
      }
    ];

    service.listPublishedGames().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].id).toBe(1);
    });

    const req = http.expectOne(r => r.method === 'GET' && r.url === '/api/notebook/games');
    req.flush(mock);
  });
});

