package dataRepo.downloads;

import java.io.Serializable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class VideoFormateInfo implements Serializable {
    @Id
    public Long id = 0L;
}
