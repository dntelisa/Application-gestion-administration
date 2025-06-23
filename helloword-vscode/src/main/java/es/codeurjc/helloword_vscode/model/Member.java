package es.codeurjc.helloword_vscode.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Member {
    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	long id;
    private String name;
    private String surname;
    private String pwd;

    // A collection of roles assigned to the user
    @ElementCollection(fetch = FetchType.EAGER)
	private List<String> roles;

    // A list of membership types associated with the user
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberType> memberTypes = new ArrayList<>();

    /* Default constructor */
    public Member() {}


    /**
     * Parameterized constructor to initialize the user with name, surname, password, and roles.
     *
     * @param name The name of the user
     * @param surname The surname of the user
     * @param pwd The password of the user
     * @param roles The roles assigned to the user
    */
    public Member(String name, String surname, String pwd, String... roles) {
        this.name = name;
        this.surname = surname;
        this.pwd = pwd;
        this.roles = List.of(roles);
    }

    
    // Getters and Setters //

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

    public List<Association> getAssociations() {
        return memberTypes.stream()
                          .map(MemberType::getAssociation)
                          .collect(Collectors.toList());
    }

    public Map<Association, String> getAssociationsWithRoles() {
        return memberTypes.stream()
                          .collect(Collectors.toMap(MemberType::getAssociation, MemberType::getName));
    }

    public List<MemberType> getMemberTypes() {
        return memberTypes;
    }
    
    public List<Minute> getMinutes() {
        return memberTypes.stream()
            .flatMap(mt -> mt.getAssociation().getMinutes().stream())
            .filter(minute -> minute.getParticipants().contains(this))
            .collect(Collectors.toList());
    }

    public void setMemberTypes(List<MemberType> memberTypes) {
        this.memberTypes = memberTypes;
    }
    
}
