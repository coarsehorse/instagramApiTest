package service;

import dto.SessionDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.CookieStore;
import java.io.*;
import java.util.Optional;

/**
 * Created by nani71 on 06/11/2019
 */
@Slf4j
public class SessionDataService {

    public static void saveSessionData(String login, String uuid, CookieStore cookieStore) {
        SessionDataDTO sessionDataDTO = new SessionDataDTO(uuid, cookieStore);
        File sessionDataFile = new File(login + ".dat");

        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(sessionDataFile));
            oos.writeObject(sessionDataDTO);
            oos.close();
            log.info("Session data has been saved successfully: " + login + ", " + uuid + ", " + cookieStore);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error on saving session data: " + login + ", " + uuid + ", " + cookieStore);
        }
    }

    public static Optional<SessionDataDTO> loadSessionData(String login) {
        File sessionDataFile = new File(login + ".dat");
        SessionDataDTO sessionDataDTO;

        if (!sessionDataFile.exists()) {
            return Optional.empty();
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(sessionDataFile));
            sessionDataDTO = (SessionDataDTO) ois.readObject();
            ois.close();
            log.info("Session data has been loaded successfully: " + login + ", " + sessionDataDTO);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            log.error("Error on loading session data: " + login);
            return Optional.empty();
        }

        return Optional.of(sessionDataDTO);
    }
}
