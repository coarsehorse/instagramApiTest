package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by nani71 on 07/11/2019
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsDTO {

    private String login;
    private String password;
}
