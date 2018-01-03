package com.tompaulus;

import com.opencsv.CSVReader;
import lombok.extern.log4j.Log4j;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.*;

/**
 * @author Tom Paulus
 * Created on 1/2/18.
 */
@Log4j
public class Main {
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final Properties properties = new Properties();

    public static void main(String[] args) throws UnsupportedEncodingException {
        String csv_path = null;
        String template_path = null;

        for (String arg : args) {
            if (arg.endsWith(".csv")) csv_path = arg;
            else if (arg.endsWith(".eml")) template_path = arg;
        }

        if (csv_path == null || csv_path.isEmpty() ||
                template_path == null || template_path.isEmpty())
            throw new RuntimeException("CSV Path or Template Path not defined");

        // Load Config
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME));
        } catch (IOException e) {
            log.fatal("Cannot load properties from file - " + CONFIG_FILE_NAME);
            return;
        }


        // Load & Split Recipients List
        List<Recipient> recipients;
        try {
            recipients = getRecipients(csv_path);
            log.info(String.format("Loaded %d recipients", recipients.size()));
        } catch (IOException e) {
            log.error("Could not load Merge Contacts & Attributes", e);
            return;
        }

        for (Recipient recipient : recipients) {
            generateAndSend(new File(template_path), recipient);
        }
    }

    /**
     *
     * @param fileName {@link String} File Path, relative to working directory
     * @return {@link List<Recipient> } List of Recipients
     * @throws IOException File cannot be opened or accessed
     */
    private static List<Recipient> getRecipients(final String fileName) throws IOException {
        List<Recipient> recipients = new ArrayList<>();

        CSVReader reader = new CSVReader(new FileReader(fileName));
        List<String[]> mergeData = reader.readAll();
        Map<String, Integer> headers = new HashMap<>();

        String[] headerRow = mergeData.get(0);
        for (int c = 0; c < headerRow.length; c++) {
            headers.put(headerRow[c], c);
        }

        for (int r = 1; r < mergeData.size(); r++) {
            String[] row = mergeData.get(r);
            Recipient recipient = new Recipient(
                    row[headers.get("first_name")],
                    row[headers.get("last_name")],
                    row[headers.get("address")]);
            for (int c = 0; c < row.length; c++) {
                if (c == headers.get("first_name") ||
                        c == headers.get("last_name") ||
                        c == headers.get("address"))
                    continue;
                recipient.attributes.put(headerRow[c], row[c]);
            }

            recipients.add(recipient);
        }
        return recipients;
    }

    /**
     * Via the provided template and recipient, generate the message and send it to the intended recipient.
     *
     * @param template {@link File} EML Template
     * @param recipient {@link Recipient} Primary Recipient of the Message
     * @throws UnsupportedEncodingException Recipient or Sender Addresses are incorrectly encoded
     */
    private static void generateAndSend(final File template, Recipient recipient) throws UnsupportedEncodingException {
        final JtwigModel recipientModel = recipient.getModel();

        Session mailSession = Session.getDefaultInstance(properties, null);
        MimeMessage message;
        try {
            message = new MimeMessage(mailSession, new FileInputStream(template));
            MimeMultipart multipart = ((MimeMultipart) message.getContent());

            for (int i = 0; i < multipart.getCount(); i++) {
                // Parse jTwig in Body of Message
                BodyPart bodyPart = multipart.getBodyPart(i);
                JtwigTemplate boydPartTemplate = JtwigTemplate.inlineTemplate((String) bodyPart.getContent());
                bodyPart.setContent(boydPartTemplate.render(recipientModel), bodyPart.getContentType());
            }

            JtwigTemplate subjectTemplate = JtwigTemplate.inlineTemplate(message.getSubject());
            message.setSubject(subjectTemplate.render(recipientModel));
        } catch (MessagingException | IOException e) {
            log.error("Could not parse message", e);
            return;
        }

        try {
            // Prepare Message for Sending
            message.setFrom(new InternetAddress(
                    properties.getProperty("mail.from.address"),
                    properties.getProperty("mail.from.name")));

            message.setRecipient(Message.RecipientType.TO, recipient.getAddress());

            message.addRecipient(Message.RecipientType.CC, new InternetAddress(
                    properties.getProperty("mail.cc.address"),
                    properties.getProperty("mail.cc.name")));
            message.setSentDate(new Date());
        } catch (MessagingException e) {
            log.error("Could not set From and/or Recipient Addresses", e);
        }

        try {
            Transport.send(message,
                    message.getSession().getProperty("mail.user"),
                    message.getSession().getProperty("mail.password"));
            log.info("Sent message to " + recipient.address);
        } catch (MessagingException e) {
            log.error("Could not send message!", e);
        }
    }

    /**
     * POJO for Primary Email Recipient (TO)
     */
    private static class Recipient {
        private String first_name;
        private String last_name;
        private String address;
        private Map<String, String> attributes;

        public Recipient(String first_name, String last_name, String address) {
            this.first_name = first_name;
            this.last_name = last_name;
            this.address = address;
            this.attributes = new HashMap<>();
        }

        public InternetAddress getAddress() throws UnsupportedEncodingException {
            return new InternetAddress(address, first_name + " " + last_name);
        }

        public JtwigModel getModel() {
            JtwigModel model = JtwigModel.newModel();
            model.with("first_name", first_name);
            model.with("last_name", last_name);
            model.with("name", first_name + " " + last_name);
            for (String key : attributes.keySet()) {
                model.with(key, attributes.get(key));
            }

            return model;
        }
    }
}
