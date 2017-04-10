package ru.ncteam.levelchat.service;


import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.ncteam.levelchat.entity.Message;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@WebListener
public class ChatService implements ServletContextListener {

    final static Logger LOGGER = Logger.getLogger(ChatService.class);

    public void contextDestroyed(final ServletContextEvent sce) {

    }

    public void contextInitialized(final ServletContextEvent sce) {
        System.setProperty("rootPath", sce.getServletContext().getRealPath("/"));

        Thread t = new Thread(new Runnable() {
            public void run() {
                final Queue<AsyncContext> chatUsers = new ConcurrentLinkedQueue<AsyncContext>();
                sce.getServletContext().setAttribute("chatUsers", chatUsers);

                Queue<Message> messages = new ConcurrentLinkedQueue<Message>();

                sce.getServletContext().setAttribute("messages", messages);

                Executor messageExecutor = Executors.newCachedThreadPool();
                final Executor chatExecutor = Executors.newCachedThreadPool();

                while (true) {
                    if (!messages.isEmpty()) {
                        final Message message = messages.poll();
                        messageExecutor.execute(new Runnable() {
                            public void run() {
                                while (!chatUsers.isEmpty()) {
                                    final AsyncContext aCtx = chatUsers.poll();
                                    chatExecutor.execute(new Runnable() {
                                        public void run() {
                                            try {
                                                ServletResponse response = aCtx.getResponse();
                                                response.setContentType("text/json");
                                                response.getWriter().write(messageAsJSON(message));
                                                aCtx.complete();
                                                LOGGER.info("Сообщение отправлено");
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        private String messageAsJSON(final Message message) {
                                            JSONObject json = new JSONObject();
                                            json.put("username", message.username);
                                            json.put("message", message.message);
                                            return json.toString();
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });

        t.start();
    }
}
