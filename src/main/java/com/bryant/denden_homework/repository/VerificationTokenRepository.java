package com.bryant.denden_homework.repository;

import com.bryant.denden_homework.entity.TokenType;
import com.bryant.denden_homework.entity.VerificationToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenAndType(String token, TokenType type);

    Optional<VerificationToken> findFirstByUser_EmailAndType(String email, TokenType type);

    /** Still-valid (un-used) tokens of a type for a user — used to invalidate prior OTPs. */
    List<VerificationToken> findByUser_EmailAndTypeAndUsedFalse(String email, TokenType type);
}
