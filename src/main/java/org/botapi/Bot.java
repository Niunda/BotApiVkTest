package org.botapi;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static Properties properties = new Properties();

    static {
        try {
        properties.load(new FileInputStream("src/main/resources/vk.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        log.info("Application has started");

        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);
        GroupActor actor = new GroupActor(Long.parseLong(properties.getProperty("groupID")),
                properties.getProperty("accessToken"));

        try {
            log.info("Bot has started");
            messagesForwarder(vk, actor);
        }
        catch (ClientException | ApiException | InterruptedException e){
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void messagesForwarder(VkApiClient vk, GroupActor actor) throws ClientException, ApiException, InterruptedException {
        Integer ts = vk.messages().getLongPollServer(actor).execute().getTs();
        while (true) {
            MessagesGetLongPollHistoryQuery historyQuery = vk.messages().getLongPollHistory(actor).ts(ts);
            List<Message> messageList = historyQuery.execute().getMessages().getItems();

            if(!messageList.isEmpty()) {
                messageList.forEach(message -> {
                    log.info("Message: " + message);
                    try {
                        vk.messages().sendDeprecated(actor).message("Вы сказали: " + message.getText())
                                .userId(message.getFromId()).randomId(message.getId()).execute();
                    }
                    catch (ApiException | ClientException e) {
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                });
            }

            ts = vk.messages().getLongPollServer(actor).execute().getTs();
            Thread.sleep(500);
        }
    }
}