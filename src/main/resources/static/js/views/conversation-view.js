define(function(require) {
	
	"use strict";
	
	var Backbone = require('backbone');
	var _ = require('underscore');
	var ConversationModel = require("models/conversation");
	require('libs/prettify/prettify');
	
	var ConversationView = Backbone.View.extend({
		el : '#conversationSection',
		initialize : function(){
			$("#run").unbind('click').bind("click", function(view){
				return function(){view.run.call(view);};
			}(this));
			this.$el.find("#saveConversationBtn").click(function(view){
				return function(){view.saveOrUpdateConversation.call(view);};
			}(this));
		},
		run : function(){
			$.ajax({
				url : APP.config.baseUrl + '/processor',
				type : 'post',
				dataType : 'json',
				contentType : "application/json",
				success : function(response) {
					console.log("####" + response);
					$("#response-wrapper").html('<pre class="prettyprint">'+ response.body+ '</pre>');
					if(response.headers && response.headers.length > 0){
						$("#res-header-wrapper").html('');
						for(var i = 0 ; i < response.headers.length; i++){
							$("#res-header-wrapper").append('<pre class="prettyprint">'+JSON.stringify(response.headers[i])+ '</pre>');
						}
					}
					prettyPrint();
					
					
				},
				data : JSON.stringify(this.getProcessRequest())
			});
		},
		getProcessRequest : function(){
			var item = {
					apiUrl : this.$el.find("#apiUrl").val(),
					methodType : this.$el.find(".apiRequestType").val(),
					apiBody : this.$el.find("#apiBody").val()
				};
			return item;
		},
		render : function(conversation) {
			console.log('conversation rendnig with model');
			console.log(conversation);
			var request = conversation.get('rfRequest');
			var response = conversation.get('rfResponse');
			
			this.$el.find("#apiRequestName").html(conversation.get('name'));
			this.$el.find("#apiRequestDescription").html(conversation.get('description'));	
			
			this.$el.find("#apiUrl").val(request.apiUrl);
			this.$el.find(".apiRequestType").val(request.methodType);
			this.$el.find("#apiBody").val(request.apiBody);
			
			this.$el.find("#response-wrapper").html('');
		},
		saveOrUpdateConversation : function(){
			if(APP.appView.getCurrentConversationId() != null){
				var rfRequest = {
						apiUrl : this.$el.find("#apiUrl").val(),
						apiBody : this.$el.find("#apiBody").val(),
						methodType : this.$el.find(".apiRequestType").val()
				}
				var rfResponse = {
						
				}
				var conversation = new ConversationModel({
					id : APP.appView.getCurrentConversationId(),
					rfRequestDTO : rfRequest,
					rfResponseDTO : rfResponse
					
				});
				conversation.save(null, {
					success: function(){
						alert('Changes saved successfully!');
					},
					error : function(){
						alert('some error occured while saving the request');
					}
				});
			}else{
				$("#requestModal").find("#source").val("conversation");
				$("#requestModal").modal("show");
			}
		}
	});
	return ConversationView;
});