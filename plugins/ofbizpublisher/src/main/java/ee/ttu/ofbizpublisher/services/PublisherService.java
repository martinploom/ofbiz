package ee.ttu.ofbizpublisher.services;

import com.google.gson.Gson;
import ee.taltech.bigdata.filtering.FilteredSearchService;
import ee.ttu.ofbizpublisher.OfbizPublisherServices;
import ee.ttu.ofbizpublisher.model.PublisherDTO;
import ee.ttu.ofbizpublisher.mqtt.ConnectionBinding;
import ee.ttu.ofbizpublisher.mqtt.Publisher;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.*;
import java.util.stream.Collectors;

public class PublisherService {

    Delegator delegator;
    DispatchContext dispatchContext;

    public PublisherService(Delegator delegator) {
        this.delegator = delegator;
    }

    public PublisherService(Delegator delegator, DispatchContext dispatchContext) {
        this.delegator = delegator;
        this.dispatchContext = dispatchContext;
    }

    public List<PublisherDTO> getPublishers() throws GenericEntityException {
        List<GenericValue> genericValues = EntityQuery.use(delegator).from("OfbizPublisher").queryList();
        return genericValues.stream().map(x -> getOfbizPublisherDTO((String) x.get("OfbizPublisherId"))).collect(Collectors.toList());
    }

    public PublisherDTO getOfbizPublisherDTO(String ofbizPublisherId) {
        PublisherDTO ofbizPublisherDTO = new PublisherDTO();
        GenericValue ofbizPublisher = null;
        try {
            ofbizPublisher = EntityQuery
                    .use(delegator)
                    .from("OfbizPublisher")
                    .where("OfbizPublisherId", ofbizPublisherId)
                    .queryOne();
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        ofbizPublisherDTO.setPublisherId((String) ofbizPublisher.get("OfbizPublisherId"));
        ofbizPublisherDTO.setEntityName((String) ofbizPublisher.get("OfbizEntityName"));
        ofbizPublisherDTO.setTopic((String) ofbizPublisher.get("topic"));
        ofbizPublisherDTO.setDescription((String) ofbizPublisher.get("description"));
        ofbizPublisherDTO.setFilter((String) ofbizPublisher.get("filter"));
        return ofbizPublisherDTO;
    }

    public void createPublisher(Map<String, Object> data) throws Exception {
        Map<String, Object> publisherContext = new HashMap<>();
        publisherContext.put("OfbizPublisherId", data.get("OfbizPublisherId"));
        publisherContext.put("OfbizEntityName", data.get("OfbizEntityName"));
        publisherContext.put("topic", data.get("topic"));
        publisherContext.put("description", data.get("description"));
        publisherContext.put("filter", data.get("filter"));
        setPublisherData(data.get("OfbizEntityName").toString(), data.get("topic").toString(), data.get("filter").toString());
        OfbizPublisherServices ofbizPublisherServices = new OfbizPublisherServices();
        ofbizPublisherServices.createOfbizPublisher(delegator, publisherContext);
    }

    public void setPublisherData(String entityName, String topic, String filterParams) throws Exception {
        Gson gson = new Gson();
        Object filter = gson.fromJson(filterParams, Object.class);
        List<List<Filter>> filterList = (List<List<Filter>>) filter;
        List<GenericValue> genericValues = new ArrayList<>();
        String model = delegator.getModelReader().getModelEntity(entityName).getEntityName();
        for (List<Filter> query : filterList) {
            SearchFilter searchFilter = new SearchFilter(query, model);
            genericValues.addAll((Collection<? extends GenericValue>) FilteredSearchService.performFilteredSearch(dispatchContext, (Map<String, ?>) searchFilter));
        }
        if (filterList.isEmpty()) {
            genericValues = EntityQuery.use(delegator).from(entityName).queryList();
        }
        String publisherId = UUID.randomUUID().toString();
        IMqttClient publisher = new MqttClient("tcp://mqtt.eclipse.org:1883", publisherId);
        ConnectionBinding mqttClientService = new ConnectionBinding(publisher);
        mqttClientService.makeConnection();
        Publisher mqttService = new Publisher(publisher, topic);
        mqttService.call(genericValues);
    }

    public void setPublisherDataWithPublisher(String entityName, String topic, String filterParams) throws Exception {
        Gson gson = new Gson();
        Object filter = gson.fromJson(filterParams, Object.class);
        List<List<Filter>> filterList = (List<List<Filter>>) filter;
        List<GenericValue> genericValues = new ArrayList<>();
        for (List<Filter> query : filterList) {
            genericValues.addAll(EntityQuery.use(delegator).from(entityName).where(query).queryList());
        }
        if (filterList.isEmpty()) {
            genericValues = EntityQuery.use(delegator).from(entityName).queryList();
        }
        GenericValue ofbizPublisher = EntityQuery
                .use(delegator)
                .from("OfbizPublisher")
                .where("topic", topic)
                .queryFirst();
        Publisher publisher = (Publisher) ofbizPublisher;
        publisher.callWithTopic(genericValues, topic);
    }

    public GenericValue deletePublisher(String ofbizPublisherId) throws GenericEntityException {
        checkPublisher(ofbizPublisherId);
        GenericValue genericValue = EntityQuery
                .use(delegator)
                .from("OfbizPublisher")
                .where("OfbizPublisherId", ofbizPublisherId)
                .queryOne();
        genericValue.remove();
        return genericValue;
    }

    private void checkPublisher(String ofbizPublisherId) throws GenericEntityException {
        GenericValue ofbizPublisher = EntityQuery
                .use(delegator)
                .from("OfbizPublisher")
                .where("OfbizPublisherId", ofbizPublisherId)
                .queryOne();
        if (ofbizPublisher == null) {
            ServiceUtil.returnError("No ofbizPublisher found!");
        }
    }
}
