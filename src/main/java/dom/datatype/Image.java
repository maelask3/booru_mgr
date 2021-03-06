package dom.datatype;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Image implements Serializable {

    private transient Path path;
    private String id = UUID.randomUUID().toString();
    private String mimetype;

    public Image(){
        // bean constructor
    }

    public byte[] getFile() throws IOException{
        return Files.readAllBytes(path);
    }

    public void setFile(byte[] file) throws IOException {
        path = Files.createTempFile(id,".tmp");
        path.toFile().deleteOnExit();
        Files.write(path, file);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getPseudofilename(){

        return id + "." + getSupposedExtension();
    }

    public String getSupposedExtension(){
        if(getMimetype().equals("image/jpeg")){
            return "jpg";
        } else {
            return getMimetype().replaceAll(".*/","");
        }
    }

    public InputStream getImageInputStream() throws IOException {
        return Files.newInputStream(path);
    }
}
