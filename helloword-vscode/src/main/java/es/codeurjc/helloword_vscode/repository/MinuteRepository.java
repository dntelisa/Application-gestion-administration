package es.codeurjc.helloword_vscode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.codeurjc.helloword_vscode.model.Minute;
import es.codeurjc.helloword_vscode.model.Member;

import java.util.List;

/**
 This interface extends JpaRepository to provide CRUD operations for the Minute entity
**/
public interface MinuteRepository extends JpaRepository<Minute, Long> {
    /* Find all Minute entities that contain the specified participant */
    List<Minute> findAllByParticipantsContains(Member participant);

    @Query("SELECT m FROM Minute m JOIN m.participants p WHERE p.id = :memberId")
    List<Minute> findAllByParticipantId(@Param("memberId") Long memberId);

}
