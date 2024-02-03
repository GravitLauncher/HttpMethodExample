package pro.gravit.launchermodules.myhttp;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;
import pro.gravit.launcher.request.auth.details.AuthTotpDetails;
import pro.gravit.launcher.request.auth.password.Auth2FAPassword;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.request.auth.password.AuthTOTPPassword;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.session.UserSessionSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.auth.texture.JsonTextureProvider;
import pro.gravit.launchserver.helper.HttpHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

public class MyHttpAuthCoreProvider extends AuthCoreProvider implements AuthSupportHardware {
    private static final int CODE_TOKEN_EXPIRED = 1001;
    private static final int CODE_INVALID_REFRESH_TOKEN = 1002;
    private transient final Logger logger = LogManager.getLogger();
    public String userByUsername;
    public String userByUuid;
    public String refreshAccessToken;
    public String authorize;
    public String userByToken;
    public String checkServer;
    public String joinServer;
    public String bearerToken;

    public String getHardwareInfoByPublicKeyUrl;
    public String getHardwareInfoByDataUrl;
    public String getHardwareInfoByIdUrl;
    public String createHardwareInfoUrl;
    public String connectUserAndHardwareUrl;
    public String addPublicKeyToHardwareInfoUrl;
    public String getUsersByHardwareInfoUrl;
    public String banHardwareUrl;
    public String unbanHardwareUrl;
    private transient HttpRequester requester;
    @Override
    public User getUserByUsername(String username) {
        try {
            var response = requester.send(requester.get(userByUsername.replace("%username%", URLEncoder.encode(username, StandardCharsets.UTF_8)), bearerToken),
                    MyHttpUser.class);
            if(response.isSuccessful()) {
                return response.result();
            }
            if(response.statusCode() == 404) {
                return null;
            }
            logger.error("getUserByUsername: {}", response.error().toString());
            return null;
        } catch (Throwable e) {
            logger.error("getUserByUsername", e);
            return null;
        }
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        try {
            var response = requester.send(requester.get(userByUuid.replace("%uuid%", uuid.toString()), bearerToken),
                    MyHttpUser.class);
            if(response.isSuccessful()) {
                return response.result();
            }
            if(response.statusCode() == 404) {
                return null;
            }
            logger.error("getUserByUUID: {}", response.error().toString());
            return null;
        } catch (Throwable e) {
            logger.error("getUserByUUID", e);
            return null;
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        HttpHelper.HttpOptional<MyHttpUserSession, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(userByToken, new GetUserByAccessTokenRequest(accessToken), bearerToken),
                    MyHttpUserSession.class);
        } catch (Throwable e) {
            logger.error("getUserSessionByOAuthAccessToken", e);
            return null;
        }
        if(response.isSuccessful()) {
            return response.result();
        }
        if(response.error().code == CODE_TOKEN_EXPIRED) {
            throw new OAuthAccessTokenExpired();
        }
        logger.error("getUserSessionByOAuthAccessToken: {}", response.error().toString());
        return null;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        HttpHelper.HttpOptional<MyHttpUserSession, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(refreshAccessToken, new RefreshTokenRequest(refreshToken), bearerToken),
                    MyHttpUserSession.class);
        } catch (Throwable e) {
            logger.error("refreshAccessToken", e);
            return null;
        }
        if(response.isSuccessful()) {
            return response.result().toAuthReport();
        }
        if(response.error().code == CODE_INVALID_REFRESH_TOKEN) {
            return null;
        }
        logger.error("refreshAccessToken: {}", response.error().toString());
        return null;
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        if(login == null || password == null) {
            throw new AuthException("Empty login or password");
        }
        String plainPassword;
        String totpCode;
        if(password instanceof AuthPlainPassword authPlainPassword) {
            plainPassword = authPlainPassword.password;
            totpCode = null;
        } else if(password instanceof Auth2FAPassword auth2FAPassword) {
            if(auth2FAPassword.firstPassword instanceof AuthPlainPassword authPlainPassword) {
                plainPassword = authPlainPassword.password;
            } else {
                throw new AuthException("Unsupported password type (first)");
            }
            if(auth2FAPassword.secondPassword instanceof AuthTOTPPassword authTOTPPassword) {
                totpCode = authTOTPPassword.totp;
            } else {
                throw new AuthException("Unsupported password type (second)");
            }
        } else {
            throw new AuthException("Unsupported password type");
        }
        HttpHelper.HttpOptional<MyHttpUserSession, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(authorize, new AuthorizeRequest(login, plainPassword, totpCode), bearerToken),
                    MyHttpUserSession.class);
        } catch (Throwable e) {
            logger.error("authorize", e);
            throw new AuthException("Unexpected server error. Please contact administrator");
        }
        if(response.isSuccessful()) {
            return response.result().toAuthReport();
        }
        throw new AuthException(response.error().error);
    }

    @Override
    public void init(LaunchServer server) {
        requester = new HttpRequester();
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        HttpHelper.HttpOptional<MyHttpUser, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(checkServer, new CheckServerRequest(username, serverID), bearerToken),
                    MyHttpUser.class);
        } catch (Throwable e) {
            logger.error("checkServer", e);
            throw new AuthException("Unexpected server error. Please contact administrator");
        }
        if(response.isSuccessful()) {
            return response.result();
        }
        throw new AuthException(response.error().error);
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) throws IOException {
        HttpHelper.HttpOptional<Void, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(joinServer, new JoinServerRequest(username, uuid, accessToken, serverID), bearerToken),
                    Void.class);
        } catch (Throwable e) {
            logger.error("joinServer", e);
            throw new AuthException("Unexpected server error. Please contact administrator");
        }
        if(response.isSuccessful()) {
            return true;
        }
        throw new AuthException(response.error().error);
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return List.of(new AuthPasswordDetails(), new AuthTotpDetails("UNKNOWN", 6));
    }

    @Override
    public void close() {

    }

    @Override
    public MyHttpUserHardware getHardwareInfoByPublicKey(byte[] publicKey) {
        HttpHelper.HttpOptional<MyHttpUserHardware, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(getHardwareInfoByPublicKeyUrl, new HardwareInfoRequest(publicKey), bearerToken), MyHttpUserHardware.class);
        } catch (Throwable e) {
            logger.error("getHardwareInfoByPublicKey", e);
        }

        if (response != null && response.statusCode() == 200) {
            MyHttpUserHardware userHardware = response.result();
            logger.debug("Successfully got UserHardware {} by publicKey", userHardware.id);
            return userHardware;
        }

        // HTTP 204 meaning NO CONTENT, that usually means no data found, but request processed successfully
        if (response != null && response.statusCode() == 204) {
            logger.debug("UserHardware not found by publicKey");
            return null;
        }

        logger.debug("Something went wrong while getting UserHardware by publicKey");
        return null;
    }

    @Override
    public MyHttpUserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info) {
        HttpHelper.HttpOptional<MyHttpUserHardware, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(getHardwareInfoByDataUrl, new HardwareInfoRequest(info), bearerToken), MyHttpUserHardware.class);
        } catch (Throwable e) {
            logger.error("getHardwareInfoByData", e);
        }

        if (response != null && response.statusCode() == 200) {
            MyHttpUserHardware userHardware = response.result();
            logger.debug("Successfully got UserHardware {} by info", userHardware.id);
            return userHardware;
        }

        // HTTP 204 meaning NO CONTENT, that usually means no data found, but request processed successfully
        if (response != null && response.statusCode() == 204) {
            logger.debug("UserHardware not found by publicKey");
            return null;
        }

        logger.debug("Something went wrong while getting UserHardware by info");
        return null;
    }

    @Override
    public MyHttpUserHardware getHardwareInfoById(String id) {
        HttpHelper.HttpOptional<MyHttpUserHardware, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(getHardwareInfoByIdUrl, new HardwareInfoRequest(id), bearerToken), MyHttpUserHardware.class);
        } catch (Throwable e) {
            logger.error("getHardwareInfoById", e);
        }

        if (response != null && response.statusCode() == 200) {
            MyHttpUserHardware userHardware = response.result();
            logger.debug("Successfully got UserHardware by id {}", id);
            return userHardware;
        }

        // HTTP 204 meaning NO CONTENT, that usually means no data found, but request processed successfully
        if (response != null && response.statusCode() == 204) {
            logger.debug("UserHardware not found by publicKey");
            return null;
        }

        logger.debug("Something went wrong while getting UserHardware by id {}", id);
        return null;
    }

    @Override
    public MyHttpUserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo info, byte[] publicKey) {
        HttpHelper.HttpOptional<MyHttpUserHardware, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(createHardwareInfoUrl, new HardwareInfoRequest(info, publicKey), bearerToken), MyHttpUserHardware.class);
        } catch (Throwable e) {
            logger.error("createHardwareInfo", e);
        }

        if (response != null && response.isSuccessful()) {
            MyHttpUserHardware userHardware = response.result();
            logger.debug("Successfully created UserHardware");
            return userHardware;
        }

        logger.debug("Something went wrong while creating UserHardware");

        // shouldn't actually happen
        throw new SecurityException("Please contact administrator");
    }

    @Override
    public void connectUserAndHardware(UserSession userSession, UserHardware hardware) {
        HttpHelper.HttpOptional<MyHttpSimpleResponse, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(connectUserAndHardwareUrl, new UserHardwareRequest((MyHttpUserHardware) hardware, (MyHttpUserSession) userSession), bearerToken), MyHttpSimpleResponse.class);
        } catch (Throwable e) {
            logger.error("connectUserAndHardware", e);
        }

        if (response != null && response.isSuccessful()) {
            logger.debug("Successfully connected user {} to hardware", userSession.getUser().getUUID());
            return;
        }

        logger.debug("Something went wrong while connecting user {} to hardware", userSession.getUser().getUUID());
    }

    @Override
    public void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey) {
        HttpHelper.HttpOptional<MyHttpSimpleResponse, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(addPublicKeyToHardwareInfoUrl, new UserHardwareRequest((MyHttpUserHardware) hardware, publicKey), bearerToken), MyHttpSimpleResponse.class);
        } catch (Throwable e) {
            logger.error("addPublicKeyToHardwareInfo", e);
        }

        if (response != null && response.isSuccessful()) {
            logger.debug("Successfully connected user hardware {} to public key", hardware.getId());
            return;
        }

        logger.debug("Something went wrong while connecting user hardware {} to public key", hardware.getId());
    }

    @Override
    public Iterable<User> getUsersByHardwareInfo(UserHardware hardware) {
        HttpHelper.HttpOptional<List<MyHttpUser>, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(getUsersByHardwareInfoUrl, new UserHardwareRequest((MyHttpUserHardware) hardware), bearerToken), (new TypeToken<List<MyHttpUser>>(){}).getType());
        } catch (Throwable e) {
            logger.error("getUsersByHardwareInfo", e);
        }

        if (response != null && response.isSuccessful()) {
            logger.debug("Successfully got users by hardware info");
            return response.result()
                    .stream()
                    .map(user -> (User) user)
                    .toList();
        }

        logger.debug("Something went wrong while connecting user hardware {} to public key", hardware.getId());
        // possible case, but it's better to return empty list from the backend to avoid debug message
        return Collections.emptyList();
    }

    @Override
    public void banHardware(UserHardware hardware) {
        HttpHelper.HttpOptional<MyHttpSimpleResponse, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(banHardwareUrl, new UserHardwareRequest((MyHttpUserHardware) hardware), bearerToken), MyHttpSimpleResponse.class);
        } catch (Throwable e) {
            logger.error("banHardware", e);
        }

        if (response != null && response.isSuccessful()) {
            logger.debug("Successfully banned hardware with id {}", hardware.getId());
            return;
        }

        logger.debug("Something went wrong while bannind hardware with id {}", hardware.getId());
    }

    @Override
    public void unbanHardware(UserHardware hardware) {
        HttpHelper.HttpOptional<MyHttpSimpleResponse, HttpRequester.SimpleError> response = null;
        try {
            response = requester.send(requester.post(unbanHardwareUrl, new UserHardwareRequest((MyHttpUserHardware) hardware), bearerToken), MyHttpSimpleResponse.class);
        } catch (Throwable e) {
            logger.error("unbanHardware", e);
        }

        if (response != null && response.isSuccessful()) {
            logger.debug("Successfully banned hardware with id {}", hardware.getId());
            return;
        }

        logger.debug("Something went wrong while bannind hardware with id {}", hardware.getId());
    }

    public record JoinServerRequest(String username, UUID uuid, String accessToken, String serverId) {}

    public record CheckServerRequest(String username, String serverId) {}

    public record AuthorizeRequest(String login, String password, String totpCode) {}

    public record RefreshTokenRequest(String refreshToken) {}

    public record GetUserByAccessTokenRequest(String accessToken) {}

    public record HardwareInfoRequest(HardwareReportRequest.HardwareInfo info, byte[] publicKey, String id) {
        HardwareInfoRequest(HardwareReportRequest.HardwareInfo info) {
            this(info, null, null);
        }

        HardwareInfoRequest(byte[] publicKey) {
            this(null, publicKey, null);
        }

        HardwareInfoRequest(String id) {
            this(null, null, id);
        }

        HardwareInfoRequest(HardwareReportRequest.HardwareInfo info, byte[] publicKey) {
            this(info, publicKey, null);
        }
    }

    public record UserHardwareRequest(MyHttpUserHardware hardware, MyHttpUserSession userSession, byte[] publicKey) {
        UserHardwareRequest(MyHttpUserHardware hardware) {
            this(hardware, null, null);
        }

        UserHardwareRequest(MyHttpUserHardware hardware, byte[] publicKey) {
            this(hardware, null, publicKey);
        }

        UserHardwareRequest(MyHttpUserHardware hardware, MyHttpUserSession userSession) {
            this(hardware, userSession, null);
        }
    }



    public record MyHttpUserSession(String id, String accessToken, String refreshToken, int expire, MyHttpUser user, String hardwareId, UserHardware userHardware) implements UserSession, UserSessionSupportHardware {

        @Override
        public String getID() {
            return id;
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public String getMinecraftAccessToken() {
            return accessToken;
        }

        @Override
        public long getExpireIn() {
            return expire*1000L; // Seconds to milliseconds
        }

        public AuthManager.AuthReport toAuthReport() {
            return new AuthManager.AuthReport(accessToken, accessToken, refreshToken, expire*1000L /* seconds to milliseconds */, this);
        }

        @Override
        public String getHardwareId() {
            return hardwareId;
        }

        @Override
        public UserHardware getHardware() {
            return userHardware;
        }
    }

    public record MyHttpUser(String username, UUID uuid, List<String> permissions, List<String> roles, Map<String, JsonTextureProvider.JsonTexture> assets, boolean banned, UserHardware userHardware) implements User, UserSupportTextures {

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public ClientPermissions getPermissions() {
            return new ClientPermissions(roles, permissions);
        }

        @Override
        public boolean isBanned() {
            return banned;
        }

        @Override
        public Texture getSkinTexture() {
            JsonTextureProvider.JsonTexture texture = assets.get("SKIN");
            if(texture != null) {
                return texture.toTexture();
            }
            return null;
        }

        @Override
        public Texture getCloakTexture() {
            JsonTextureProvider.JsonTexture texture = assets.get("CAPE");
            if(texture != null) {
                return texture.toTexture();
            }
            return null;
        }

        @Override
        public Map<String, Texture> getUserAssets() {
            return JsonTextureProvider.JsonTexture.convertMap(assets);
        }
    }

    public record MyHttpUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, String id, boolean banned) implements UserHardware {

        @Override
        public HardwareReportRequest.HardwareInfo getHardwareInfo() {
            return hardwareInfo;
        }

        @Override
        public byte[] getPublicKey() {
            return publicKey;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isBanned() {
            return banned;
        }
    }

    public record MyHttpSimpleResponse(String message) {
    }
}
