package es.codeurjc.helloword_vscode.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.rowset.serial.SerialBlob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import es.codeurjc.helloword_vscode.model.Association;
import es.codeurjc.helloword_vscode.model.Member;
import es.codeurjc.helloword_vscode.model.MemberType;
import es.codeurjc.helloword_vscode.model.Minute;
import es.codeurjc.helloword_vscode.repository.AssociationRepository;
import es.codeurjc.helloword_vscode.repository.MemberRepository;
import es.codeurjc.helloword_vscode.repository.MemberTypeRepository;
import es.codeurjc.helloword_vscode.repository.MinuteRepository;
import jakarta.annotation.PostConstruct;

/**
 * This service class is used to initialize the database with predefined data, including users,
 * associations, roles, and minutes. It is executed after the application context is loaded.
*/
@Service
public class DataInitializer {


    // Autowired repositories for database interactions //

    @Autowired
    private MemberRepository MemberRepository;

    @Autowired
    private MinuteRepository minuteRepository;

    @Autowired
    private MemberTypeRepository roleRepository;

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /* Initialize the database with predefined data */
    @PostConstruct
    public void init() throws SQLException, IOException {

        Member member1 = new Member("Jean", "Jan", passwordEncoder.encode("mdp"), "USER");
        Member member2 = new Member("Pierre", "Pro", passwordEncoder.encode("pwd"), "USER", "ADMIN");
        Member member3 = new Member("Luc", "lds", passwordEncoder.encode("aaa"), "USER");
        MemberRepository.saveAll(Arrays.asList(member1, member2, member3));

        // Add associations
        Association association1 = new Association("GreenPeace");
        associationRepository.save(association1); 
        Association association2 = new Association("GreatSchool");
        associationRepository.save(association2);
        Association association3 = new Association("Help");
        associationRepository.save(association3);

        // Add roles
        MemberType role1 = new MemberType("secretary", member1, association1);
        roleRepository.save(role1);
        MemberType role2 = new MemberType("president", member2, association1);
        roleRepository.save(role2);
        MemberType role3 = new MemberType("president", member2, association2);
        roleRepository.save(role3);

        // Add minutes
        Minute minute1 = new Minute("2023-10-01", Arrays.asList(member1, member2), "New actions about climat", 60.0, association1);
        minuteRepository.save(minute1);
        Minute minute2 = new Minute("2024-11-03", Arrays.asList(member1, member2), "Discussion on government measures", 30.0, association1);
        minuteRepository.save(minute2);

        // Creation of members
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            members.add(new Member("Name" + i, "Surname" + i, passwordEncoder.encode("pass" + i), "USER"));
        }
        MemberRepository.saveAll(members);

        // Associations to create with still image
        List<String> names = List.of("Love Earth", "Give Smile", "Construct Avenir", "Culture Club", "Nature Warrior", "Book Lovers");
        List<String> imageFiles = List.of("image1.jpg", "image2.jpg", "image3.jpg", "image4.jpg", "image5.jpg", "image6.jpg");

        List<Association> associations = new ArrayList<>();

        for (int i = 0; i < names.size(); i++) {
            Association asso = new Association(names.get(i));

            // Image in order
            ClassPathResource imgFile = new ClassPathResource("static/images/asso/" + imageFiles.get(i));
            byte[] bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            asso.setImageFile(new SerialBlob(bytes));
            asso.setImage(true);

            associationRepository.save(asso);
            associations.add(asso);
        }

        // Distribution of roles in each association
        List<MemberType> roles = new ArrayList<>();
        int memberIndex = 0;

        for (Association asso : associations) {
            // 1 president
            roles.add(new MemberType("president", members.get(memberIndex++), asso));

            // 0 or 1 secretary
            if (memberIndex < members.size() && memberIndex % 2 == 0) {
                roles.add(new MemberType("secretary", members.get(memberIndex++), asso));
            }

            // 0 or 1 treasurer
            if (memberIndex < members.size() && memberIndex % 3 == 0) {
                roles.add(new MemberType("treasurer", members.get(memberIndex++), asso));
            }

            // 0 or 1 vice-president
            if (memberIndex < members.size() && memberIndex % 4 == 0) {
                roles.add(new MemberType("vice-president", members.get(memberIndex++), asso));
            }

            // Add 3 to five members
            int numMembers = 3 + (memberIndex % 3); // 3, 4 ou 5
            for (int j = 0; j < numMembers && memberIndex < members.size(); j++) {
                roles.add(new MemberType("member", members.get(memberIndex++), asso));
            }
        }

        roleRepository.saveAll(roles);

        // Create minute for each 
        List<Minute> minutes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Association asso = associations.get(i % associations.size());

            // Recover all the members with a role in this association
            List<Member> eligibleMembers = roles.stream()
                .filter(r -> r.getAssociation().equals(asso))
                .map(MemberType::getMember)
                .distinct()
                .toList();

            if (eligibleMembers.size() < 2) continue; // Not enough participant

            // Choose 2 participants
            Member m1 = eligibleMembers.get(0);
            Member m2 = eligibleMembers.get(1);

            Minute minute = new Minute(
                "2024-0" + ((i % 9) + 1) + "-0" + ((i % 27) + 1),
                Arrays.asList(m1, m2),
                "Minute n°" + (i + 1),
                30.0 + i,
                asso
            );
            minutes.add(minute);
        }

        minuteRepository.saveAll(minutes);

        for (Minute minute : minutes) {
            for (Member participant : minute.getParticipants()) {
                participant.getMinutes().add(minute);
                MemberRepository.save(participant);
            }
        }

    }
}