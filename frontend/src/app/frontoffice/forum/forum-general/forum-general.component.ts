import { Component, OnInit } from '@angular/core';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';

@Component({
  selector: 'app-forum-general',
  templateUrl: './forum-general.component.html',
  styleUrls: ['./forum-general.component.css']
})
export class ForumGeneralComponent implements OnInit {
  topics: ForumTopic[] = [];
  filteredTopics: ForumTopic[] = [];
  loading = false;
  
  // Search and filter properties
  searchQuery: string = '';
  sortBy: 'title' | 'category' | 'views' | 'createdAt' = 'createdAt';
  sortOrder: 'asc' | 'desc' = 'desc';
  
  // Pagination properties
  currentPage: number = 1;
  itemsPerPage: number = 9;
  totalPages: number = 1;
  
  Math = Math; // Expose Math to template

  constructor(private forumTopicService: ForumTopicService) { }

  ngOnInit() {
    this.loading = true;
    this.forumTopicService.getGeneralTopics().subscribe({
      next: (data) => {
        this.topics = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  applyFilters(): void {
    // Apply search filter
    let filtered = this.topics;
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(topic => 
        topic.title.toLowerCase().includes(query) ||
        topic.description.toLowerCase().includes(query) ||
        topic.category.toLowerCase().includes(query)
      );
    }

    // Apply sorting
    filtered = [...filtered].sort((a, b) => {
      let aValue: any, bValue: any;
      
      switch (this.sortBy) {
        case 'title':
          aValue = a.title.toLowerCase();
          bValue = b.title.toLowerCase();
          break;
        case 'category':
          aValue = a.category.toLowerCase();
          bValue = b.category.toLowerCase();
          break;
        case 'views':
          aValue = a.views;
          bValue = b.views;
          break;
        case 'createdAt':
          aValue = new Date(a.createdAt || '').getTime();
          bValue = new Date(b.createdAt || '').getTime();
          break;
        default:
          return 0;
      }

      if (aValue < bValue) return this.sortOrder === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    // Calculate pagination
    this.totalPages = Math.ceil(filtered.length / this.itemsPerPage);
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.filteredTopics = filtered.slice(startIndex, endIndex);
  }

  onSearchChange(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  onSortChange(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.applyFilters();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = Math.min(5, this.totalPages);
    let startPage = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }
}
