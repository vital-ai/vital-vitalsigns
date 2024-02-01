package ai.vital.vitalservice

import ai.vital.vitalservice.query.ResultList

interface QueueConsumer {

    void messageReceived(String queueName,  String consumerID, String partitionID, List<String> partitionList, ResultList msg)
    
}
