import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { ForumMessageService, ForumMessage, CreateMessageRequest } from '../../../core/services/forum-message.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-topic-detail',
  templateUrl: './topic-detail.component.html',
  styleUrls: ['./topic-detail.component.css']
})
export class TopicDetailComponent implements OnInit {
  topic: ForumTopic | null = null;
  messages: ForumMessage[] = [];
  messageReplies: { [key: number]: ForumMessage[] } = {};
  newMessageContent = '';
  replyContent = '';
  replyingTo: number | null = null;
  editingMessageId: number | null = null;
  editContent = '';
  currentUserId = 1;

  constructor(
    private route: ActivatedRoute,
    private topicService: ForumTopicService,
    private messageService: ForumMessageService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.loadTopic(id);
    this.loadMessages(id);
  }

  loadTopic(id: number) {
    this.topicService.getTopicById(id).subscribe(t => this.topic = t);
  }

  loadMessages(topicId: number) {
    this.messageService.getMessagesByTopic(topicId).subscribe(msgs => {
      this.messages = msgs.filter(m => !m.parentMessageId);
      this.messages.forEach(m => this.loadReplies(m.id!));
    });
  }

  loadReplies(messageId: number) {
    this.messageService.getReplies(messageId).subscribe(
      replies => this.messageReplies[messageId] = replies
    );
  }

  postMessage() {
    if (!this.topic || !this.newMessageContent.trim()) return;
    const req: CreateMessageRequest = {
      topicId: this.topic.id!,
      authorId: this.currentUserId,
      content: this.newMessageContent,
      parentMessageId: null
    };
    this.messageService.createMessage(req).subscribe({
      next: () => {
        this.snackBar.open('✓ Message posted successfully', 'Close', { duration: 3000 });
        this.newMessageContent = '';
        this.loadMessages(this.topic!.id!);
      },
      error: () => this.snackBar.open('✕ Error posting message', 'Close', { duration: 3000 })
    });
  }

  replyTo(msgId: number) {
    this.replyingTo = msgId;
    this.replyContent = '';
  }

  submitReply(parentId: number) {
    if (!this.topic || !this.replyContent.trim()) return;
    const req = {
      topicId: this.topic.id!,
      authorId: this.currentUserId,
      content: this.replyContent,
      parentMessageId: parentId
    };
    this.messageService.createMessage(req).subscribe({
      next: () => {
        this.snackBar.open('✓ Reply sent successfully', 'Close', { duration: 3000 });
        this.replyingTo = null;
        this.replyContent = '';
        this.loadReplies(parentId);
      },
      error: () => this.snackBar.open('✕ Error sending reply', 'Close', { duration: 3000 })
    });
  }

  startEdit(message: ForumMessage) {
    this.editingMessageId = message.id!;
    this.editContent = message.content;
  }

  saveEdit(messageId: number) {
    if (!this.editContent.trim()) return;
    this.messageService.updateMessage(messageId, this.editContent).subscribe({
      next: () => {
        this.snackBar.open('✓ Message updated successfully', 'Close', { duration: 3000 });
        this.editingMessageId = null;
        this.loadMessages(this.topic!.id!);
      },
      error: () => this.snackBar.open('✕ Error updating message', 'Close', { duration: 3000 })
    });
  }

  deleteMessage(messageId: number) {
    if (confirm('Are you sure you want to delete this message?')) {
      this.messageService.deleteMessage(messageId).subscribe({
        next: () => {
          this.snackBar.open('✓ Message deleted successfully', 'Close', { duration: 3000 });
          this.loadMessages(this.topic!.id!);
        },
        error: () => this.snackBar.open('✕ Error deleting message', 'Close', { duration: 3000 })
      });
    }
  }
}
