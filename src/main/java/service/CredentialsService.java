package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.CredentialsDTO;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by nani71 on 07/11/2019
 */
@Slf4j
public class CredentialsService {

    public static Optional<CredentialsDTO> readCredentialsFromFile(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();

        CredentialsDTO credentials;
        try {
            credentials = objectMapper.readValue(new File(filePath), CredentialsDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error on read credentials");
            return Optional.empty();
        }

        log.info("Successfully read credentials");
        return Optional.of(credentials);
    }
}
