package es.codeurjc.helloword_vscode.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.model.MemberType;

/**
 This interface extends JpaRepository to provide CRUD operations for the MemberType entity
**/
public interface MemberTypeRepository extends JpaRepository<MemberType, Long> {
    /* Find MemberType entities by the associated Member */
    List<MemberType> findByMember(Member member);

    List<MemberType> findByMemberIdAndAssociationId(Long memberId, Long associationId);

    List<MemberType> findByAssociationIdAndNameIgnoreCase(Long associationId, String name);


}
