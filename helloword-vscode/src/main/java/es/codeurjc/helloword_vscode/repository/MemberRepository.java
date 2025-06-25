package es.codeurjc.helloword_vscode.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.codeurjc.helloword_vscode.model.Member;

/**
 This interface extends JpaRepository to provide CRUD operations for the UtilidateurEntity entity
**/
public interface MemberRepository extends JpaRepository<Member, Long> {
    /* Find an Member by their name */
    Optional<Member> findByName(String name);

    @Query("SELECT DISTINCT m FROM Member m JOIN m.memberTypes mt WHERE mt.association.id = :associationId")
    List<Member> findMembersByAssociationId(@Param("associationId") Long associationId);

}
