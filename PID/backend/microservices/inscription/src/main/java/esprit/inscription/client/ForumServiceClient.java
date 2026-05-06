package esprit.inscription.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "forum")
public interface ForumServiceClient {

    @GetMapping("/api/forum/topics/user/{userId}")
    List<ForumTopic> getUserTopics(@PathVariable("userId") Long userId);

    @GetMapping("/api/forum/messages/user/{userId}")
    List<ForumMessage> getUserMessages(@PathVariable("userId") Long userId);

    @GetMapping("/api/forum/topics/level/{level}")
    List<ForumTopic> getTopicsByLevel(@PathVariable("level") String level);

    @GetMapping("/api/forum/stats/user/{userId}")
    UserForumStats getUserStats(@PathVariable("userId") Long userId);

    // DTOs for the response
    class ForumTopic {
        private Long id;
        private String title;
        private String description;
        private String category;
        private Long authorId;
        private Integer views;
        private String createdAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        
        public Integer getViews() { return views; }
        public void setViews(Integer views) { this.views = views; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    class ForumMessage {
        private Long id;
        private String content;
        private Long authorId;
        private Long topicId;
        private String createdAt;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        
        public Long getTopicId() { return topicId; }
        public void setTopicId(Long topicId) { this.topicId = topicId; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    class UserForumStats {
        private Long userId;
        private Integer topicCount;
        private Integer messageCount;
        private Integer totalViews;
        private String lastActivity;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Integer getTopicCount() { return topicCount; }
        public void setTopicCount(Integer topicCount) { this.topicCount = topicCount; }
        
        public Integer getMessageCount() { return messageCount; }
        public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
        
        public Integer getTotalViews() { return totalViews; }
        public void setTotalViews(Integer totalViews) { this.totalViews = totalViews; }
        
        public String getLastActivity() { return lastActivity; }
        public void setLastActivity(String lastActivity) { this.lastActivity = lastActivity; }
    }
}
