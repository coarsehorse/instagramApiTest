import dto.CredentialsDTO;
import lombok.extern.slf4j.Slf4j;
import org.brunocvcunha.instagram4j.Instagram4j;
import org.brunocvcunha.instagram4j.requests.*;
import org.brunocvcunha.instagram4j.requests.payload.*;
import service.CredentialsService;
import service.SessionDataService;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by nani71 on 05/11/2019
 */

@Slf4j
public class InstagramApiTest {

    public static void main(String[] args) throws Exception {
        CredentialsDTO credentials = CredentialsService
                .readCredentialsFromFile("credentials.json")
                .orElseThrow(() -> new Exception("Cannot read credentials from file!"));

        Instagram4j instagram4j = login(credentials)
                .orElseThrow(() -> new Exception("Cannot login"));

        // Send direct message

        StatusResult sendMessageResult = instagram4j.sendRequest(InstagramDirectShareRequest
                .builder()
                .shareType(InstagramDirectShareRequest.ShareType.MESSAGE)
                .recipients(Arrays.asList("4018040809"))
                .message("Hello insta world again")
                .build()
        );

        log.info("Message sending result: " + sendMessageResult.getStatus());

        // Get comments on the photo

//        InstagramGetMediaCommentsResult getMediaCommentsResult = instagram4j
//                .sendRequest(new InstagramGetMediaCommentsRequest("2170829562420939502_5873697792", null));
//
//        log.info("Media comments result: " + getMediaCommentsResult.getComments());

        // Get pending

//        InstagramPendingInboxResult instagramPendingInboxResult =
//                instagram4j.sendRequest(new InstagramGetPendingInboxRequest(null));
//
//        log.info("" + instagramPendingInboxResult.getInbox());

        // Get inbox

//        InstagramInboxResult instagramInboxResult =
//                instagram4j.sendRequest(new InstagramGetInboxRequest(null));
//
//        log.info("instagramInboxResult: " /*+ instagramInboxResult.getInbox()*/);
//
        // Get pending

//        InstagramPendingInboxResult instagramPendingInboxResult =
//                instagram4j.sendRequest(new InstagramGetPendingInboxRequest(instagramInboxResult.getInbox().getThreads().get(0).getNewest_cursor()));
//
//        log.info("Newest cursor: " + instagramPendingInboxResult.getInbox());

        // Upload photo
//        InstagramConfigurePhotoResult instagramConfigurePhotoResult =
//                instagram4j.sendRequest(new InstagramUploadPhotoRequest(
//                    new File("C:/Users/User/Desktop/insta_photo.jpg"),
//                    "How am I doing TT?"));
//
//        log.info("Photo: " + instagramConfigurePhotoResult.toString());
    }

    /**
     * Login with handling instagram challenge(confirmation via sms).
     *
     * @return Optional with Instagram API object on success, empty Optional otherwise.
     */
    private static Optional<Instagram4j> login(CredentialsDTO credentials) {
        Instagram4j instagram4j = SessionDataService
                .loadSessionData(credentials.getLogin())
                .map(sessionDataDTO -> Instagram4j
                        .builder()
                        .uuid(sessionDataDTO.getUuid())
                        .cookieStore(sessionDataDTO.getCookieStore())
                        .username(credentials.getLogin())
                        .password(credentials.getPassword())
                        .build()
                )
                .orElse(Instagram4j
                        .builder()
                        .username(credentials.getLogin())
                        .password(credentials.getPassword())
                        .build()
                );

        instagram4j.setup();

        try {
            InstagramLoginResult instagramLoginResult = instagram4j.login();

            if (Objects.equals(instagramLoginResult.getStatus(), "ok")) {
                log.info("Logged in as usual");
                SessionDataService.saveSessionData(
                        credentials.getLogin(), instagram4j.getUuid(), instagram4j.getCookieStore()
                );
                return Optional.of(instagram4j);
            }
            else if (Objects.equals(instagramLoginResult.getError_type(), "checkpoint_challenge_required")) {
                // Challenge required

                // Get challenge URL
                String challengeUrl = instagramLoginResult.getChallenge().getApi_path().substring(1);

                // Reset challenge
                String resetChallengeUrl = challengeUrl
                        .replace("challenge", "challenge/reset");
                InstagramGetChallengeResult getChallengeResult =
                        instagram4j.sendRequest(new InstagramResetChallengeRequest(resetChallengeUrl));

                // If action is close
                if (Objects.equals(getChallengeResult.getAction(), "close")) {
                    // Get challenge
                    getChallengeResult = instagram4j
                            .sendRequest(new InstagramGetChallengeRequest(challengeUrl));
                }

                if (Objects.equals(getChallengeResult.getStep_name(), "select_verify_method")) {
                    // Get security code
                    InstagramSelectVerifyMethodResult postChallengeResult = instagram4j
                            .sendRequest(new InstagramSelectVerifyMethodRequest(challengeUrl,
                                    getChallengeResult.getStep_data().getChoice()));

                    System.out.print("Please, input the security code from SMS (XXX XXX) without spaces: ");

                    String securityCode = null;
                    try (Scanner scanner = new Scanner(System.in)) {
                        securityCode = scanner.nextLine();
                    }

                    // Send security code
                    InstagramLoginResult securityCodeInstagramLoginResult = instagram4j
                            .sendRequest(new InstagramSendSecurityCodeRequest(challengeUrl, securityCode));

                    if (Objects.equals(securityCodeInstagramLoginResult.getStatus(), "ok")) {
                        log.info("Logged in successfully after passing the challenge");
                        SessionDataService.saveSessionData(
                                credentials.getLogin(), instagram4j.getUuid(), instagram4j.getCookieStore()
                        );
                        return Optional.of(instagram4j);
                    } else {
                        log.error("Cannot login even after passing the challenge");
                        return Optional.empty();
                    }
                }
            } else {
                log.error("Cannot login as usual + checkpoint challenge is not required");
                return Optional.empty();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
