/*
 * Copyright 2016 Tuntuni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tuntuni.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;
import org.tuntuni.Core;
import org.tuntuni.connection.Client;
import org.tuntuni.models.Message;

/**
 * The controller for text messaging and file sharing.
 * <p>
 * It gives a history based text conversation window. Below is a text box and
 * send text button. Above is the conversation history. </p>
 */
public class MessagingController implements Initializable, ListChangeListener<Message> {

    private Client mClient;

    @FXML
    private TextArea messageText;
    @FXML
    private ListView messageList;
    @FXML
    private Label errorLabel;
    @FXML
    private Label userName;
    @FXML
    private ImageView userPhoto;

    private final double MINIMUM_HEIGHT = 70D;
    private final double MAXIMUM_HEIGHT = 150D;

    public MessagingController() {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Core.instance().messaging(this);
    }

    public void setClient(Client client) {
        if (mClient != null) {
            mClient.messageProperty().removeListener(this);
        }
        mClient = client;
        Platform.runLater(() -> loadAll());
    }

    private void loadAll() {
        messageText.clear();
        errorLabel.setText("");
        messageList.getItems().clear();
        // load user name and avatar 
        if (mClient == null) {
            userName.setVisible(false);
            return;
        }
        // show user info
        if (mClient.getUserData() != null) {
            userName.setVisible(true);
            userName.setText(mClient.getUserData().getUserName());
            userPhoto.setImage(mClient.getUserData().getAvatar(
                    userPhoto.getFitWidth(), userPhoto.getFitHeight()));
        }
        // load list of past messages
        showHistory(0);
        mClient.messageProperty().addListener(this);
    }

    @Override
    public void onChanged(Change<? extends Message> c) {
        c.next();
        if (c.wasAdded()) {
            showHistory(c.getFrom());
        }
    }

    public void showHistory(int from) {
        // iterate list
        for (int i = from; i < mClient.messageProperty().size(); ++i) {
            final Message message = mClient.messageProperty().get(i);
            Platform.runLater(() -> {
                // add element
                messageList.getItems().add(MessageBox.createInstance(message));
            });
        }
        Platform.runLater(() -> {
            // show last
            int last = messageList.getItems().size() - 1;
            if (last > 0) {
                messageList.scrollTo(last);
            }
        });
    }

    private String sendMessage(String text) {
        try {
            Message message = new Message();
            message.setText(text);
            message.setViewed(true);
            mClient.sendMessage(message);
            messageText.clear();
            return "Message sent!";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void notifyIncoming(Message message) {
        if (message == null || message.isViewed()) {
            return;
        }        
        String title = message.getSender().getUserName() + " sent a message!";
        String msg = message.getText();
        if (msg.length() > 200) {
            msg = msg.substring(0, 200) + "...";
        }
        Action action = new Action("View", (ActionEvent t) -> {
            Platform.runLater(() -> {
                Core.instance().main().showUser(mClient);
                Core.instance().main().selectMessaging();
            });
        });

        Notifications.create()
                .darkStyle()
                .title(title)
                .text(msg)
                .action(action)
                .hideAfter(Duration.seconds(20))
                .showInformation();
    }

    @FXML
    private void messageKeyPressed(KeyEvent evt) {
        //setTextAreaHeight();
        if (evt.getCode() == KeyCode.ENTER) {
            evt.consume();
            if (evt.isShiftDown()) {
                messageText.appendText("\n");
            } else {
                handleSendMessage(null);
            }
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        errorLabel.setText("");
        String text = messageText.getText().trim();

        if (text.isEmpty()) {
            errorLabel.setText("Message is too short");
            return;
        }

        if (mClient == null) {
            errorLabel.setText("The receiver is unknown");
            return;
        }

        String res = sendMessage(text);
        errorLabel.setText(res);
    }

    @Deprecated
    private void setTextAreaHeight() {
        // elements
        Region content = (Region) messageText.lookup(".content");
        ScrollBar scrollBarv = (ScrollBar) messageText.lookup(".scroll-bar:vertical");
        // get the height of content
        double height = MINIMUM_HEIGHT;
        if (messageText.getText().length() > 10) {
            Insets padding = messageText.getPadding();
            height = content.getHeight() + padding.getTop() + padding.getBottom();
        }
        // set height
        height = Math.max(MINIMUM_HEIGHT, height);
        height = Math.min(MAXIMUM_HEIGHT, height);
        messageText.setMinHeight(height);
        messageText.setPrefHeight(height);
        // enable or the scroll bar
        scrollBarv.setVisibleAmount(MAXIMUM_HEIGHT);
    }

}
