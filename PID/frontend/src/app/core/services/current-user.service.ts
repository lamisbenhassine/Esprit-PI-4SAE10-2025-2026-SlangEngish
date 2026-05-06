import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private userId = 1;

  getUserId(): number {
    return this.userId;
  }

  setUserId(id: number): void {
    this.userId = id;
  }
}

