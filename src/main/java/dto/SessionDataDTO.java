package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.client.CookieStore;
import java.io.Serializable;

/**
 * Created by nani71 on 06/11/2019
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionDataDTO implements Serializable {

    private String uuid;
    private CookieStore cookieStore;
}
