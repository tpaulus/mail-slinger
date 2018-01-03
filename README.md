Mail Slinger
============

While plenty solutions exist to distribute mass emails to a set of email
addresses, filling in custom information for each message (mail merge), few, if
any, solutions exist to CC a user on all of those emails, especially when paired
with a custom SMTP Host (not gmail). While this may seem like an unusual use
case, it does have some practical applications, especially in the age of meeting
scheduling services, like [x.ai](https://x.ai), which operate via email and
being notified via being CCd on an email.

This relatively simple Java program does just that, taking in a CSV file with
the recipient list and merge attributes, and an `.eml` file containing the email
message you would like to send. Running it is as easy as executing the jar file
(which you will need to build once you have entered your desired values in the
`config.properties` file) via the JVM and the line below:

```sh
java -jar build/libs/mail-slinger-1.0.jar template.eml emails.csv
```

The arguments can be provided in any order, with the argument type being
determined via the file extension; either `.eml` for the template or `.csv` for
the recipient list.

## Making the Message Template
Most email clients will let you compose a draft, save that
draft, and then let you save it as an `.eml` file from there; dragging and
dropping the message to your desktop often will have the same affect. The
message can have individual content injected via [jTwig](http://jtwig.org) tags,
the most common of which in this application will be the double curly braces,
which surround the attribute name; for example: `{{first_name}}`. By default,
the fields `first_name`, `last_name`, and `name` are included based off of the
required fields in the recipient list, however additional attributes can be
added by including more columns in the recipient list, with the header column
value becoming the attribute name.

## Recipient List
The recipient list is provided in the form of a CSV file, which must include a
header row, which is used to identify the contents of each column. For the most
part columns can have an arbitrary heading, although the following three headers
are required:
- `first_name`
- `last_name`
- `address`

## Configuration
A sample properties file is located in `src/main/resources/sample_config.properties`
which can be duplicated and renamed to `config.properties` also in the resources
folder. From there, you can modify the SMTP host, port (make sure you set both
values correctly, they will usually be the same), and who should be CCed on all
of the messages.

Once this is done, you can build the `.jar` file using the included Gradle
Wrapper via the line below. You may need to make the Gradle wrapper executable,
if you encounter permission issues.

```sh
./gradlew clean build fatJar
```

Gradle will save the resulting `.jar` file in `build/libs` from where it can
either be executed or saved elsewhere for future execution.
