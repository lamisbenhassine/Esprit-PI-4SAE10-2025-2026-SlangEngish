package esprit.forum.service;

import esprit.forum.entity.UserBlock;
import esprit.forum.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumBlockService {

    private final UserBlockRepository userBlockRepository;

    public Set<Long> getBlockedUserIds(Long blockerUserId) {
        if (blockerUserId == null) {
            return Set.of();
        }
        return userBlockRepository.findByBlockerUserId(blockerUserId).stream()
                .map(UserBlock::getBlockedUserId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public UserBlock blockUser(Long blockerUserId, Long blockedUserId) {
        if (blockerUserId == null || blockedUserId == null) {
            throw new IllegalArgumentException("blockerUserId et blockedUserId sont requis");
        }
        if (blockerUserId.equals(blockedUserId)) {
            throw new IllegalArgumentException("Impossible de bloquer votre propre compte (modérateur et auteur identiques).");
        }
        return userBlockRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId)
                .orElseGet(() -> {
                    UserBlock b = new UserBlock();
                    b.setBlockerUserId(blockerUserId);
                    b.setBlockedUserId(blockedUserId);
                    return userBlockRepository.save(b);
                });
    }

    @Transactional
    public void unblockUser(Long blockerUserId, Long blockedUserId) {
        userBlockRepository.deleteByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId);
    }

    public boolean isBlocked(Long blockerUserId, Long authorId) {
        if (blockerUserId == null || authorId == null) {
            return false;
        }
        return userBlockRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, authorId).isPresent();
    }
}
