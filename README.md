# census-fwmt-events

## Purpose
Library for handling events and creating events for the purpose of Acceptance Testing, Error Handling, and Logging.

#### Error Handling
When a GatewayException occurs it is handled by taking the records and adding it to a queue for processing at a later date.
Item is are added to queue and transactions logged.

#### Acceptance Testing
This library is key for the acceptance testing to validate records.
Records get processed and then the acceptance tests interrogate event queue for the results.

//TODO - Add flag to enable for all environments except production.

#### Logging
Every time an event occurs it is then logged as part of the process. These logs are used for creation of the Log metrics

## Testing
To test locally build jar to local using
```
  ./gradlew clean publishToMavenLocal
```


