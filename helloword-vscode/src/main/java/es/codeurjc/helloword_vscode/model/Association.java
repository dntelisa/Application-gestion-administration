package es.codeurjc.helloword_vscode.model;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;

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


    /**
     * Parameterized constructor to initialize the association with a name and an image file.
     *
     * @param name The name of the association.
     * @param imgAsso The image file path or identifier for the association.
    */
    public Association(String name, String imgAsso) {
        this.name = name;
        this.memberTypes = new ArrayList<>();
    }

    /**
     * Parameterized constructor to initialize the association with a name.
     *
     * @param name The name of the association.
     */
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
}

