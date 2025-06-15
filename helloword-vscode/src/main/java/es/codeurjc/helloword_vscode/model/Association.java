package es.codeurjc.helloword_vscode.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import java.sql.Blob;

import jakarta.persistence.CascadeType;

@Entity
public class Association {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String name;

    @Lob
    private Blob imageFile;

    private String imagePath; 

    private boolean image;

    @OneToMany(mappedBy = "association", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberType> memberTypes = new ArrayList<>();
    
    @OneToMany(mappedBy = "association", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Minute> minutes = new ArrayList<>();   


    /* Default constructor */
    public Association() {}


    /* Constructor with name and image file*/
    public Association(String name, String imgAsso) {
        this.name = name;
        this.memberTypes = new ArrayList<>();
    }

    /* Constructor only with name */
    public Association(String name) {
        this.name = name;
        this.memberTypes = new ArrayList<>();
    }


    // Getters and Setters //

    public Blob getImageFile() {
		return imageFile;
	}

	public void setImageFile(Blob imageFile) {
		this.imageFile = imageFile;
	}

    public boolean getImage(){
		return this.image;
	}

    public void setImage(boolean image){
		this.image = image;
	}

    public void setImagePath(String imagePath){
		this.imagePath = imagePath;
	}

    public String getImagePath(){
		return imagePath;
	}

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

    public List<MemberType> getMemberTypes() {
        return memberTypes;
    }

    public void setMemberTypes(List<MemberType> memberTypes) {
        this.memberTypes = memberTypes;
    }

    public List<Minute> getMinutes() {
        return minutes;
    }

    public void setMinutes(List<Minute> minutes) {
        this.minutes = minutes;
    }

    // DO NOT DO THIS
    public List<Member> getMembers() {
        // Retrieve all users associated with this association
        return memberTypes.stream()
                     .map(MemberType::getMember)
                     .collect(Collectors.toList());
    }

    // DO NOT DO THIS
    // public void setMembers(List<Member> members) {
    //     // Assure that all users has a role in their association
    //     this.memberTypes = members.stream()
    //                         .map(member -> {
    //                             MemberType memberType = new MemberType();
    //                             memberType.setMember(member);
    //                             memberType.setAssociation(this);
    //                             return memberType;
    //                         })
    //                         .collect(Collectors.toList());
    // }
}

