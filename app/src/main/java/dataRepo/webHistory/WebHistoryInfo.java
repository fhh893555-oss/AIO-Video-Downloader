package dataRepo.webHistory;

import java.io.Serializable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public final class WebHistoryInfo implements Serializable {
	@Id public long id = 0L;
}
