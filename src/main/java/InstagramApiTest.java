import dto.CredentialsDTO;
import lombok.extern.slf4j.Slf4j;
import org.brunocvcunha.instagram4j.Instagram4j;
import org.brunocvcunha.instagram4j.requests.*;
import org.brunocvcunha.instagram4j.requests.payload.*;
import service.CredentialsService;
import service.SessionDataService;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by nani71 on 05/11/2019
 */

@Slf4j
public class InstagramApiTest {

    public static void main(String[] args) throws Exception {
        Instagram4j instagram4j = setup();

        // Send direct message
        StatusResult sendMessageResult = doRequestAndSaveSession(instagram4j, InstagramDirectShareRequest
                .builder()
                .shareType(InstagramDirectShareRequest.ShareType.MESSAGE)
                .recipients(Collections.singletonList("4018040809"))
                .message("Hello again, do we meeting today?")
                .build());

//        if (!Objects.equals(sendMessageResult.getStatus(), "ok")) {
//            // Try to relogin
//            instagram4j = login(instagram4j)
//                    .orElseThrow(() -> new Exception("Cannot login, check [ERROR] logs"));
//        }

        log.info("Message sending result: " + sendMessageResult.getStatus());

        // Get comments on the photo
//        InstagramGetMediaCommentsResult getMediaCommentsResult = doRequestAndSaveSession(instagram4j,
//                new InstagramGetMediaCommentsRequest("2170829562420939502_5873697792", null));
//
//        log.info("Media comments result: " + getMediaCommentsResult.getComments());

        // Get pending
//        InstagramPendingInboxResult instagramPendingInboxResult =
//                doRequestAndSaveSession(instagram4j, new InstagramGetPendingInboxRequest(null));
//
//        log.info("" + instagramPendingInboxResult.getInbox());

        // Get inbox
//        InstagramInboxResult instagramInboxResult =
//                doRequestAndSaveSession(instagram4j, new InstagramGetInboxRequest(null));
//
//        log.info("instagramInboxResult :: unseen_count = {}", instagramInboxResult.getInbox().getUnseen_count());

        // Get pending
//        InstagramPendingInboxResult instagramPendingInboxResult1 =
//                doRequestAndSaveSession(instagram4j, new InstagramGetPendingInboxRequest(
//                        instagramInboxResult.getInbox().getThreads().get(0).getNewest_cursor()));
//
//        log.info("Newest cursor: " + instagramPendingInboxResult1.getInbox());

        // Upload photo
//        InstagramConfigurePhotoResult instagramConfigurePhotoResult = doRequestAndSaveSession(
//                instagram4j, new InstagramUploadPhotoRequest(
//                        new File("C:/Users/User/Desktop/China.jpg"),
//                        "Best joke - well forgotten old joke"));
//
//        log.info("Photo: " + instagramConfigurePhotoResult.toString());
    }

    /**
     * Setup Instagra4j object with proper credentials and last session data.
     */
    private static Instagram4j setup() throws Exception {
        CredentialsDTO credentials = CredentialsService
                .readCredentialsFromFile("credentials.json")
                .orElseThrow(() -> new Exception("Cannot read credentials from file"));

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
                .orElseGet(() -> Instagram4j
                        .builder()
                        .username(credentials.getLogin())
                        .password(credentials.getPassword())
                        .build()
                );

        instagram4j.setup();

        return instagram4j;
    }

    /**
     * Login with handling instagram challenge.
     *
     * @return Optional with Instagram API object on success, empty Optional otherwise.
     */
    private static Optional<Instagram4j> login(Instagram4j instagram4j) throws IOException {
        InstagramLoginResult instagramLoginResult = instagram4j.login();

        if (Objects.equals(instagramLoginResult.getStatus(), "ok")) {
            log.info("Logged in as usual");
            SessionDataService.saveSessionData(
                    instagram4j.getUsername(), instagram4j.getUuid(), instagram4j.getCookieStore()
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
            else if (Objects.equals(getChallengeResult.getStep_name(), "select_verify_method")) {
                // Get security code
                InstagramSelectVerifyMethodResult postChallengeResult = instagram4j
                        .sendRequest(new InstagramSelectVerifyMethodRequest(challengeUrl,
                                getChallengeResult.getStep_data().getChoice()));

                System.out.print("Please, input the security code from SMS/email (XXX XXX) without spaces: ");
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
                            instagram4j.getUsername(), instagram4j.getUuid(), instagram4j.getCookieStore()
                    );
                    return Optional.of(instagram4j);
                } else {
                    log.error("Cannot login even after passing the challenge");
                    return Optional.empty();
                }
            } else {
                log.error("checkpoint_challenge_required " +
                        " - unhandled case, getChallengeResult: {}", getChallengeResult);
                return Optional.empty();
            }
        } else {
            log.error("Cannot login as usual + checkpoint challenge is not required");
            return Optional.empty();
        }

        log.error("Cannot login, unhandled case, instagramLoginResult: {}", instagramLoginResult);
        return Optional.empty();
    }

    /**
     * Do request and save changed session data.
     *
     * @param instagram4j Main API object that has passed setup.
     * @param request     Instagram request.
     * @return result of request.
     */
    public static <T extends StatusResult, R extends InstagramRequest<T>> T doRequestAndSaveSession(
            Instagram4j instagram4j, R request) throws IOException {
        T statusResult = instagram4j.sendRequest(request);

        SessionDataService.saveSessionData(
                instagram4j.getUsername(), instagram4j.getUuid(), instagram4j.getCookieStore());

        return statusResult;
    }
}
