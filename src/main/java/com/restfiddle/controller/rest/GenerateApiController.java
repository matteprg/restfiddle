/*
 * Copyright 2015 Ranjan Kumar
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
package com.restfiddle.controller.rest;

import java.util.Date;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.restfiddle.dao.GenericEntityDataRepository;
import com.restfiddle.dao.GenericEntityRepository;
import com.restfiddle.dto.ConversationDTO;
import com.restfiddle.dto.NodeDTO;
import com.restfiddle.dto.RfRequestDTO;
import com.restfiddle.dto.StatusResponse;
import com.restfiddle.entity.BaseNode;
import com.restfiddle.entity.Conversation;
import com.restfiddle.entity.GenericEntity;
import com.restfiddle.entity.GenericEntityField;

@RestController
@Transactional
public class GenerateApiController {
    Logger logger = LoggerFactory.getLogger(GenerateApiController.class);

    @Autowired
    private NodeController nodeController;

    @Autowired
    private ConversationController conversationController;

    @Autowired
    private GenericEntityRepository genericEntityRepository;

    @Autowired
    private GenericEntityDataRepository genericEntityDataRepository;

    @RequestMapping(value = "/api/entities/{id}/generate-api", method = RequestMethod.POST)
    public @ResponseBody
    StatusResponse generateApiByEntityId(@PathVariable("id") String id) {
	logger.debug("Generating APIs for entity with id: " + id);

	GenericEntity entity = genericEntityRepository.findOne(id);

	String baseNodeId = entity.getBaseNodeId();
	BaseNode entityNode = nodeController.findById(baseNodeId);

	return generateApi(entityNode);
    }

    public StatusResponse generateApi(BaseNode entityNode) {
	GenericEntity genericEntity = entityNode.getGenericEntity();

	// API to GENERATE >> List of Entity Data
	ConversationDTO conversationDTO = new ConversationDTO();
	RfRequestDTO rfRequestDTO = new RfRequestDTO();

	String projectId = entityNode.getProjectId();

	rfRequestDTO.setApiUrl("http://localhost:8080/api/" + projectId + "/entities/" + entityNode.getName() + "/list");
	rfRequestDTO.setMethodType("GET");
	conversationDTO.setRfRequestDTO(rfRequestDTO);

	Conversation createdConversation = conversationController.create(conversationDTO);
	conversationDTO.setId(createdConversation.getId());

	String nodeName = "Get List of " + entityNode.getName();
	BaseNode createdNode = createNode(nodeName, entityNode.getId(), projectId, conversationDTO);

	// API to GENERATE >> Get Entity Data By Id
	conversationDTO = new ConversationDTO();
	rfRequestDTO = new RfRequestDTO();
	rfRequestDTO.setApiUrl("http://localhost:8080/api/" + projectId + "/entities/" + entityNode.getName() + "/{uuid}");
	rfRequestDTO.setMethodType("GET");
	conversationDTO.setRfRequestDTO(rfRequestDTO);

	createdConversation = conversationController.create(conversationDTO);
	conversationDTO.setId(createdConversation.getId());

	nodeName = "Get " + entityNode.getName() + " by Id";
	createdNode = createNode(nodeName, entityNode.getId(), projectId, conversationDTO);

	// API to GENERATE >> Delete Entity Data By Id
	conversationDTO = new ConversationDTO();
	rfRequestDTO = new RfRequestDTO();
	rfRequestDTO.setApiUrl("http://localhost:8080/api/" + projectId + "/entities/" + entityNode.getName() + "/{uuid}");
	rfRequestDTO.setMethodType("DELETE");
	conversationDTO.setRfRequestDTO(rfRequestDTO);

	createdConversation = conversationController.create(conversationDTO);
	conversationDTO.setId(createdConversation.getId());

	nodeName = "Delete " + entityNode.getName();
	createdNode = createNode(nodeName, entityNode.getId(), projectId, conversationDTO);

	// API to GENERATE >> Create Entity Data
	conversationDTO = new ConversationDTO();
	rfRequestDTO = new RfRequestDTO();
	rfRequestDTO.setApiUrl("http://localhost:8080/api/" + projectId + "/entities/" + entityNode.getName());
	rfRequestDTO.setMethodType("POST");

	JSONObject jsonObject = getFieldJson(genericEntity);
	// Make a pretty-printed JSON text of this JSONObject.
	rfRequestDTO.setApiBody(jsonObject.toString(4));
	conversationDTO.setRfRequestDTO(rfRequestDTO);

	createdConversation = conversationController.create(conversationDTO);
	conversationDTO.setId(createdConversation.getId());

	nodeName = "Create " + entityNode.getName();
	createdNode = createNode(nodeName, entityNode.getId(), projectId, conversationDTO);

	// API to GENERATE >> Update Entity Data
	conversationDTO = new ConversationDTO();
	rfRequestDTO = new RfRequestDTO();
	rfRequestDTO.setApiUrl("http://localhost:8080/api/" + projectId + "/entities/" + entityNode.getName() + "/{uuid}");
	rfRequestDTO.setMethodType("PUT");

	jsonObject = getFieldJson(genericEntity);
	// id is required in case of update.
	jsonObject.put("id", "{uuid}");

	rfRequestDTO.setApiBody(jsonObject.toString(4));
	conversationDTO.setRfRequestDTO(rfRequestDTO);

	createdConversation = conversationController.create(conversationDTO);
	conversationDTO.setId(createdConversation.getId());

	nodeName = "Update " + entityNode.getName();
	createdNode = createNode(nodeName, entityNode.getId(), projectId, conversationDTO);

	return null;
    }

    private BaseNode createNode(String nodeName, String parentId, String projectId, ConversationDTO conversationDTO) {
	NodeDTO childNode = new NodeDTO();
	childNode.setName(nodeName);
	childNode.setProjectId(projectId);
	childNode.setConversationDTO(conversationDTO);
	BaseNode createdNode = nodeController.create(parentId, childNode);
	return createdNode;
    }

    private JSONObject getFieldJson(GenericEntity genericEntity) {
	// Create JSON template for the entity data based on fields and set it as api body.
	List<GenericEntityField> fields = genericEntity.getFields();
	JSONObject jsonObject = new JSONObject();
	for (GenericEntityField genericEntityField : fields) {
	    String type = genericEntityField.getType();
	    if ("STRING".equalsIgnoreCase(type)) {
		jsonObject.put(genericEntityField.getName(), "");
	    } else if ("LONG".equalsIgnoreCase(type)) {
		jsonObject.put(genericEntityField.getName(), Long.valueOf(1));
	    } else if ("INTEGER".equalsIgnoreCase(type)) {
		jsonObject.put(genericEntityField.getName(), Integer.valueOf(1));
	    } else if ("DATE".equalsIgnoreCase(type)) {
		jsonObject.put(genericEntityField.getName(), new Date());
	    }
	}
	return jsonObject;
    }
}
