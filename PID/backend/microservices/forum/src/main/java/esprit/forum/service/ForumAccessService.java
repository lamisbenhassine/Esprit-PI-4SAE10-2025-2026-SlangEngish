package esprit.forum.service;

import esprit.forum.client.InscriptionClient;
import esprit.forum.entity.ForumSpace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumAccessService {

    private final InscriptionClient inscriptionClient;

    public void assertCanAccess(ForumSpace space, Long userId) {
        if (space == null) {
            throw new RuntimeException("Forum space is required");
        }
        if (space.getType() == ForumSpace.ForumSpaceType.GENERAL) {
            return;
        }
        if (userId == null) {
            throw new RuntimeException("userId is required for restricted forums");
        }
        // Espaces par niveau CECRL : ouverts aux utilisateurs connectés (démo / parité avec le front
        // « checkPayment »). L’accès par niveau (A1…C2) est déjà filtré côté UI.
        if (space.getType() == ForumSpace.ForumSpaceType.LEVEL) {
            return;
        }
        Boolean isPaid = inscriptionClient.isUserPaid(userId);
        if (isPaid == null || !isPaid) {
            throw new RuntimeException("User must have an active subscription to access this forum");
        }
    }
}

