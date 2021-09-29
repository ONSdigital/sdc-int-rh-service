# Firestore

### Firestore contention

Firestore will throw a contention exception when overloaded, with the expectation that the application will retry with
an exponential backoff. See this page for details: https://cloud.google.com/storage/docs/request-rate

CR-555 added datastore backoff support to RH service. It uses Spring @Retryable annotation in RespondentDataRepositoryImpl to
do an exponential backoff for DataStoreContentionException exceptions. If the maximum number of retries is used without
success then the 'doRecover' method throws the failing exception.


The explicit handling of contention exceptions has some advantages:

- Keeps log files clean when contention is happening, as an exception is only logged once both layers of retries have been exhausted.
- Easy analysis on the quantity of contention, as a custom retry listener does a single line log entry on completion of the retries.
- Allows large number of retries for only the expected contention. Any other issue will only have the standard 3 or so retries.
- Google state that contention can happen on reads too. If we were to get this during load testing then it's easy to
  apply the proven retryable annotation to read methods.

#### Contention logging

The logging for different contention circumstances is as follows.

**No contention**

Logging will show the arrival of the case/uac but there is no further logging if successful.

**Initial contention and then success**

A warning is logged when the object is finally stored:

    2019-12-11 08:45:32.258  INFO  50306 --- [enerContainer-1] u.g.o.c.i.r.e.i.CaseEventReceiverImpl    : Entering acceptCaseEvent
    2019-12-11 08:45:44.713  WARN  50087 --- [enerContainer-1] u.g.o.c.i.r.r.impl.CustomRetryListener   : writeCollectionCase: Transaction successful after 19 attempts

There is no logging of the contention exceptions or for each retry.

**Continual contention with retries exhausted**

If attempts to store the object result in continued contention and all retries are used then this is also logged
as a warning.

    2019-12-11 09:16:35.362  INFO  50306 --- [enerContainer-1] u.g.o.c.i.r.e.i.CaseEventReceiverImpl    : Entering acceptCaseEvent
    2019-12-11 09:18:12.336  WARN  50306 --- [enerContainer-1] u.g.o.c.i.r.r.impl.CustomRetryListener   : writeCollectionCase: Transaction failed after 30 attempts


#### Contention backoff times

The retry of Firestore puts is controlled by the following properties:

**backoffInitial** Is the number of milliseconds that the initial backoff will wait for.

**backoffMultiplier** This controls the increase in the wait time for each subsequent failure and retry.

**backoffMax** Is the maximum amount of time that we want to wait before retrying.

**backoffMaxAttempts** This limits the number of times we are going to attempt the opertation before throwing an exception.

The default values for these properties has been set to give a very slow rate escalation. This should mean that the
number of successful Firestore transactions is just a fraction below the actual maximum possible rate. The shallow
escalation also means that we will try many times, and combined with a relatively high maximum wait, will mean
that we should hopefully never see a transaction (which is failing due to contention) going back to PubSub.

Under extreme contention RH should slow down to the extent that each RH thread is only doing one Firestore add per
minute. This should mean that RH is submitting requests 100's of times slower than Firestore can handle.

#### Contention backoff times

To help tune the contention backoff configuration (see 'cloudStorage' section of application properties) here is a noddy program to help:

    public class RetryTimes {
      public static void main(String[] args) {
        long nextWaitTime = 100;
        double multiplier = 1.20;
        long maxWaitTime = 26000;
    
        int iterations = 0;
        long totalWaitTime = 0;
    
        System.out.println("iter wait   total");
    
        while (nextWaitTime < maxWaitTime) {
          iterations++;
          totalWaitTime += nextWaitTime;
      
          System.out.printf("%2d %,6d %,6d\n", iterations, nextWaitTime, totalWaitTime);
      
          nextWaitTime = (long) (nextWaitTime * multiplier);
        }
      }
    }

This helps show the backoff time escalation and the maximum run time for a transaction:

    iter wait   total
     1    100    100
     2    120    220
     3    144    364
     4    172    536
     5    206    742
     6    247    989
     7    296  1,285
     8    355  1,640
     9    426  2,066
    10    511  2,577
    11    613  3,190
    12    735  3,925
    13    882  4,807
    14  1,058  5,865
    15  1,269  7,134
    16  1,522  8,656
    17  1,826 10,482
    18  2,191 12,673
    19  2,629 15,302
    20  3,154 18,456
    21  3,784 22,240
    22  4,540 26,780
    23  5,448 32,228
    24  6,537 38,765
    25  7,844 46,609
    26  9,412 56,021
    27 11,294 67,315
    28 13,552 80,867

## Copyright
Copyright (C) 2021 Crown Copyright (Office for National Statistics)