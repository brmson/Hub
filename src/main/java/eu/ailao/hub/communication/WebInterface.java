package eu.ailao.hub.communication;
/**
 * Created by Petr Marek on 26.11.2015.
 * Class which handles connection between web interface and yodaQA
 */

import eu.ailao.hub.concepts.Concept;
import eu.ailao.hub.concepts.ConceptMemorizer;
import eu.ailao.hub.questions.Question;
import eu.ailao.hub.questions.QuestionMapper;
import eu.ailao.hub.transformations.Transformation;
import eu.ailao.hub.transformations.TransformationArray;
import eu.ailao.hub.users.User;
import eu.ailao.hub.users.UserMapper;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static eu.ailao.hub.Statics.isContain;
import static spark.Spark.*;
import static spark.Spark.get;


public class WebInterface implements Runnable {

	private int port;
	private String yodaQAURL;
	private QuestionMapper questionMapper;
	private UserMapper userMapper;

	private static final String USER_ID = "userID";

	public WebInterface(int port, String yodaQAURL) {
		this.port = port;
		this.yodaQAURL = yodaQAURL;
		this.questionMapper = new QuestionMapper();
		this.userMapper = new UserMapper();
	}

	/***
	 * Starts getting requests from web interface and sending it to yodaQA
	 */
	public void run() {
		port(port);
		post("/q", ((request, response) -> handleGettingID(request, response)));
		get("/q/*/*", ((request, response) -> handleGettingAnswer(request, response)));
		get("/q/", ((request, response) -> handleGettingInformation(request, response)));
	}

	/***
	 * Reaction to POST request to /q
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private Object handleGettingID(Request request, Response response) throws IOException, ExecutionException, InterruptedException {
		response.header("Access-Control-Allow-Origin", "*");
		response.status(201);

		Map<String, String[]> queryParamsMap = request.queryMap().toMap();

		String questionText = queryParamsMap.get("text")[0];
		Question question=new Question(questionText);
		question=transformQuestion(question);

		String[] userIDStringArr=queryParamsMap.get("userID");
		String userIDString=null;
		if (userIDStringArr!=null){
			userIDString=userIDStringArr[0];
		}
		User user= userMapper.getUser(userIDString);

		user.getConceptMemorizer().updateConceptsDuringAsking(queryParamsMap);

		String answerID= askQuestion(question, request, user.getConceptMemorizer().getConcepts());
		questionMapper.addQuestion(getQuestionIDFromAnswer(answerID),question);
		JSONObject answer = new JSONObject(answerID);
		answer.put("userID",user.getUserID());
		return answer.toString();
	}

	/***
	 * Reaction to GET request to /q/:id
	 * @param request
	 * @param response
	 * @return
	 */
	private Object handleGettingAnswer(Request request, Response response) {
		response.type("application/json");
		response.header("Access-Control-Allow-Origin", "*");
		response.status(201);

		String userID=request.splat()[1];
		User user= userMapper.getUser(userID);

		int id = Integer.parseInt(request.splat()[0]);
		CommunicationHandler communicationHandler = new CommunicationHandler();
		String GETResponse = communicationHandler.getGETResponse(yodaQAURL + "q/" + id);
		JSONObject answer = new JSONObject(GETResponse);
		answer=transformBack(id,answer);
		user.getConceptMemorizer().updateConceptsDuringGettingQuestion(answer);
		return answer.toString();
	}

	/***
	 * Reaction to GET request to /q/
	 * @param request
	 * @param response
	 * @return
	 */
	private Object handleGettingInformation(Request request, Response response) {
		String result=null;
		response.type("application/json");
		response.header("Access-Control-Allow-Origin", "*");
		response.status(201);
		CommunicationHandler communicationHandler = new CommunicationHandler();
		if (request.queryParams("toAnswer") != null) {
			result = communicationHandler.getGETResponse(yodaQAURL + "q/?toAnswer");
		} else if (request.queryParams("inProgress") != null) {
			result = communicationHandler.getGETResponse(yodaQAURL + "q/?inProgress");
		} else if (request.queryParams("answered") != null) {
			result = communicationHandler.getGETResponse(yodaQAURL + "q/?answered");
		}
		return result;
	}

	/***
	 * Detect third person pronoun in question. If it is presented, it uses concepts from older questions
	 * @param question Text of question
	 * @param request Request from web interface
	 * @return response of yodaQA
	 */
	private String askQuestion(Question question, Request request, ArrayDeque<Concept> concepts) {
		ArrayDeque<Concept> _concepts=new ArrayDeque<>();
		if (isThirdPersonPronouns(question.getTransformedQuestionText())){
			_concepts=concepts;
		}
		CommunicationHandler communicationHandler = new CommunicationHandler();
		return communicationHandler.getPOSTResponse(yodaQAURL + "/q", request, question.getTransformedQuestionText(), _concepts);
	}

	/***
	 * Detects if there is pronouns in the third person in question text.
	 * @param question Question to check for pronoun
	 * @return TRUE if there is third person pronoun in question
	 */
	private boolean isThirdPersonPronouns(String question){
		String[] thirdPersonPronouns = {"he", "she", "it", "his", "hers", "him", "her", "they", "them", "their"};
		for (int i = 0; i < thirdPersonPronouns.length; i++) {
			if (isContain(question.toLowerCase(), thirdPersonPronouns[i])) {
				return true;
			}
		}
		return false;
	}

	/***
	 * Gets ID of question from YodaQA's answer to this question
	 * @param answer YodaQA's answer
	 * @return id of question
	 */
	private int getQuestionIDFromAnswer(String answer){
		return Integer.parseInt(answer.replaceAll("[\\D]", ""));
	}

	/***
	 * Applies transformations to question
	 * Change question text according to transformations defined in TransformationArray.transformationsList
	 * @param question Question to transform
	 * @return Transformed question
	 */
	private Question transformQuestion(Question question){
		for(Transformation transformation: TransformationArray.transformationsList){
			question.applyTransformationIfUseful(transformation);
		}
		return question;
	}

	/***
	 * Transforms answer back
	 * Applies back transformations in reverse order to answer
	 * @param id id of question
	 * @param answer YodaQA's answer
	 * @return Answer transformed back
	 */
	private JSONObject transformBack(int id, JSONObject answer){
		Question question = questionMapper.getQuestionByID(id);
		return question.transformBack(answer);
	}
}
