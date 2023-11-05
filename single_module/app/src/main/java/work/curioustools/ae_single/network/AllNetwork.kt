package work.curioustools.ae_single.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * # Making Apps for Billions. Understanding some concepts that goes into creating the future
 *
 * Mobile Apps are the forefront of technology innovation in 2023. But as Indie devs, do you know
 * what empowers these apps to be so robust,reliable,performant and secure? There are many things.
 * This article revolves around them and gives a very opinionated yet general and holistic view of
 * how your favourite app is able to cater to the demands of you and million other people.
 *
 * Note: before we dive any further, this article is going to discuss about a few libraries which
 * are common in most apps. we won't be going in depth about those as those are just the tools
 * to achieve a particular functionality or feature. these libraries are also not exhaustive and
 * many alternatives exist out there which you are free to use/explore
 *
 * ## Starting from the basics : What is expected from your app?
 * - scalability, perfromance,security, compatiability, mainatinence,modularity, reliability, etc. ch
 * - checkout <BLOCK1>
 *
 * ## Dissecting an android app project
 * This is fairly trivial for any android developer who  spent more than 2 days on dev.android.com
 * website. We can usually expect the following things from a typical android project.
 * 1. it has some code in java , kotlin or C++ files which represent the logic of how the app works
 * 2. it has some xml files which could represent multiple things localisation,
 *    layouts,images, navigation, menus,styles,preferences,animations.
 * 3. it has some asset files like pdf ,image, audio/video,font,zip,db files
 * 4. finally, it has some meta files like gradle, gradle-kts,gradle wrapper and properties files which impact the compilation of an app
 *
 *
 * Now, your app could be as simple as opening an screen and doing [nothing](https://play.google.com/store/apps/details?id=com.gorro.nothing)
 * or as complex as a [video streaming + marketplace+ social media app](https://play.google.com/store/apps/details?id=com.facebook.katana)
 * but the first part of code will exist for all apps. So to adhere to the expectations we discussed earlier, we must put code in these files in a certain way
 *
 * ### Enter, the world of app architectures!
 * Android has been around for 15 years and we have seen various attempts to create a code structure which caters to the above expectations
 * I personally find MVVM Clean architecture to be my goto way of creating apps.
 *
 *
 *
 * in a nutshell: api/cache <- repo <- usecase <- viewmodel <- ui
 *
 * To further explain this, i am going to show base files and some actual code files . for the sake of simplicity, i am omitting the cache part of the architecture
 *
 */


/**
 * UserAPI : An interface class that represents the whole network communication layer. (Libs Used : Coroutines, Retrofit)
 *
 * As you can see, it has some function definitions which take some inputs and returns Response<T> class's objects as output
 *
 * 1. retrofit is the go-to library for most projects due to its ease of use, flexibility,
 *    scalability to incorporate any kind of  requests and performance. this also somewhat defines
 *    the very first layer of our architecture as an instance of this interface will be used as the
 *    single source of communication to/from the server. Note: We do not create an implementaion of
 *    this class, instead we receive this implementation from retrofit when we call
 *    `retrofit.create(UserAPI::class.java)`
 * 2. retrofit communicates with the server and returns the data in a generic Response<T> format.
 *    based on the concurrency library used, it provides responses like a
 *    direct Response<T> (used with coroutines), Call<T> (used with java based callback architecture),
 *    Single<T>,Disposable<T>, etc
 * 3. Coroutines make the network layer very simple and idiomatic, therefore we used coroutines here
 *
 */
interface UserAPI {
    @GET("api/users") suspend fun getUserList(@Query("page") pageNum: Int? = null, @Query("per_page") perPage: Int? = null, ): Response<BackendJsonStructure<List<UserResponse>>>
    @GET("/users/{id}") suspend fun getUser(@Path("id") id: Int): Response<BackendJsonStructure<UserResponse>>
    @POST("/users") suspend fun createUser(@Body data: CreateUserRequest): Response<BackendJsonStructure<CreateUserResponse>>
    @PATCH("/users/{id}") suspend fun updateUser(@Path("id") id: Int, @Body data: CreateUserRequest): Response<BackendJsonStructure<CreateUserResponse>>
}


/**
 * a helper class to provide additional common parameters to response.
 * a typical backend response is like this:
 * ```json
 * {
 *   "data": {
 *       "id": 1,
 *        "name": "Item 1"
 *      },
 *   "page": 1,
 *   "per_page": 10,
 *   "total": 30,
 *   "total_pages": 3,
 *   "error": null
 * }
 *
 * ```
 * which makes this  json structure wrapper easy to incorporate major and helpful info
 * */
data class BackendJsonStructure<T> (
    val data: T? = null,
    val page: Int = -1,
    val per_page: Int = -1,
    val total: Int = -1,
    val total_pages: Int = -1,
    val error:String? = null,
)


/** UserRepo : An interface that represents the retrieval, modification and transformation of data for business logic. (libs used : coroutine,retrofit,roomdb)
 * 1. this is the layer which will decide what data needs to passed onto further layers for business logic processing and finally the ui logic processing
 * 2. As you can see, it returns ApiResponse<T> object and not Response<T> object.
 *      - this is because Response<T> represents what we received from the network. we do not want to
 *        further emit the same data created by some 3rd party with all its meta info which the
 *        other layers are not concerned about. Retrofit is limited to this layer only
 *      - further more we may want to perform some other transformations like combining responses from different sources, or adding some meta data.
 *      - finally the data from the network and the data from the cache should be emitted out in the same , single format, so this class is used
 * 3. Notice that this is an interface and not a class. this is because we do not want to hard code the dependencies used in creating an instance of a class that could provide such functionality. in an essence we are basically saying that there is a layer which provides us with these features. and we don't care how its implemented and uses whichever dependencies needed to achieve this functionality so long as the functionality is there
 */
interface UserRepo {
    suspend fun getUserList(): ApiResponse<List<UserResponse>>
    suspend fun getUser(id: Int): ApiResponse<UserResponse>
    suspend fun createUser(data: CreateUserRequest): ApiResponse<CreateUserResponse>
    suspend fun updateUser(data: CreateUserRequest) : ApiResponse<CreateUserResponse>
}
/**
 * UserRepoImpl : the embodiment of UserRepo Layer
 */
class UserRepoRemoteImpl @Inject constructor(private val userAPI: UserAPI) : UserRepo {

    override suspend fun getUserList(): ApiResponse<List<UserResponse>> {
        return userAPI.getUserList().toApiResponse()
    }

    override suspend fun getUser(id: Int): ApiResponse<UserResponse> {
        return userAPI.getUser(id).toApiResponse()
    }

    override suspend fun createUser(data: CreateUserRequest): ApiResponse<CreateUserResponse> {
        return userAPI.createUser(data).toApiResponse()
    }

    override suspend fun updateUser(data: CreateUserRequest): ApiResponse<CreateUserResponse> {
        if (data.id == null) error("pass id when updating user. id cannot be null")
        return userAPI.updateUser(data.id, data).toApiResponse()
    }

    //to do : move to RetrofitExtensions.kt
    private fun <T> Response<BackendJsonStructure<T>>.toApiResponse() : ApiResponse<T> {
        val body: BackendJsonStructure<T>? = body()
        if (isSuccessful && body != null && body.data!=null) {
            return ApiResponse.Success(
                data = body.data,
                page = body.page,
                perPage = body.per_page,
                total = body.total,
                totalPages = body.total_pages
            )
        }
        return ApiResponse.Failure(code(), errorMsg = body?.error)
    }


}


/**
 * ApiResponse<T> : The Representation of business friendly data.
 * 1. a typical business logic around data received from internet (or even cache) is :
 *    if(api returned successful data) do x else do y. Thus we got a sealed class with  child
 *    classes Success and Failure. child layers can use the cool switch case paradigm of kotlin
 *    to easily perform any action
 * 2. another typical business logic is to show a loading progress or skeleton while api request
 *    is happening , therefore we support another class: Loading
 * 3. Finally another common business logic is to show different kinds of error based on error types.
 *    This is provided as an independent feature via ResponseCodeType enum class. user can again use the
 *    cool switch case paradigm of kotlin to easily perform any error based action
 * 4. the mapper in RepoImpl class maps the server (or cache) response to the ApiResponse classes
 *    accordingly
 *
 *
 */
sealed class ApiResponse<T> {

    class Loading<T>: ApiResponse<T>()

    data class Success<T>(
        val data: T,
        val page: Int = -1,
        val perPage: Int = -1,
        val total: Int = -1,
        val totalPages: Int = -1
    ) : ApiResponse<T>()

    data class Failure<T>(val errorCode: Int, val errorMsg: String? = null) : ApiResponse<T>() {
        fun errorCodeType() =
            ResponseCodeType.values().firstOrNull { it.code == errorCode } ?: ResponseCodeType.UNRECOGNISED
    }

    enum class ResponseCodeType(val code: Int, val defaultMsg: String) {
        SUCCESS(200, "SUCCESS"),
        NO_INTERNET_CONNECTION(1001, "No Internet found"),
        NO_INTERNET_PACKETS_RECEIVED(1002, "We are unable to connect to our server. Please check with your internet service provider"),
        USER_NOT_FOUND(400, "User Not Found"),
        APP_NULL_RESPONSE_BODY(888, "No Response found"),
        SERVER_FAILURE(500, "server failure"),
        SERVER_DOWN_502(502, "server down 502"),
        SERVER_DOWN_503(503, "server down 503"),
        SERVER_DOWN_504(504, "server down 504"),
        UNRECOGNISED(-1, "unrecognised error in networking");
    }

    companion object{
        fun <T> error(
            msg: String = ResponseCodeType.UNRECOGNISED.defaultMsg,
            code: Int = ResponseCodeType.UNRECOGNISED.code
        ): Failure<T> {
            return Failure(code, msg)
        }

        fun <T> success(data:T): Success<T> {
            return Success(data)
        }
    }
}

/**
 * UseCase: The Representation of concurrency required for data retrieval operation
 * 1. In a UI Thread based framework like android, it is very important to make sure thatUI thread
 *    remains free for UI related tasks for smooth user experience. thus it becomes a ground rule
 *    to make every  data access call from the cache or network to be asynchronous.
 * 2. So as of now, we have UserAPI and UserRepo layers which we have marked as suspend and
 *    therefore assume their work to be synchronous. from here, we need to ensure that a call to
 *    userRepo.doSomething() is made on a parallel thread. thus frameworks like coroutines, rx or
 *    Executor can be used.
 * 3. However the data returned is typically required on a main thread, so the UseCases are usually
 *    responsible for that too
 * 4. Some additional concurrency scenarios could include : cancelling an api call on button
 *    click/other event, cancelling the api call automatically after timeout,
 *    combining multiple api calls and make a final call after the results of previous calls,etc
 * 5. as you see, there can be multiple concurrency behavior, so we cannot completely generalise it.
 *    however we can make a good habit of adding all the concurrency related stuff in UseCases and not in
 *    ViewModel/ui. The little bit of generalisation can be as follows:
 */
interface UseCase


/**
 * UseCase(coroutines + livedata for transferring data)
 * 1. repo call is provided by the child class by implementing [getRepoCall]
 * 2. when some button press triggers [requestForData],the repo call is executed in io thread
 *    as a new job.
 * 3. once the execution is complete, it is passed to livedata, which can be observed(or transformed
 *    at the ViewModel and then observed) by the ui.
 * 4. additional helper functions [getCurrentAsyncRequest] and [cancelRequest] are provided to get raw
 *    job and cancel current call respectively. (todo: find examples for these methods )
 * 5. additional helper method [requestForDataSync] is provided if the caller does not wish to use
 *    the concurrency provided by UseCase and handle the concurrency on their own. This is only
 *    recommended for cases like async-await-cancelAll
 * 6. todo multi call async-await-cancel-all usecase
 */
abstract class BaseConcurrencyUseCase<REQUEST, RESP:Any> {
    private val job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    val responseLiveData = MutableLiveData<RESP>()

    abstract suspend fun getRepoCall(param: REQUEST): RESP

    fun requestForData(param: REQUEST) {
         scope.apply {
            launch(Dispatchers.IO + job) {
                val result = getRepoCall(param)
                responseLiveData.postValue(result)
            }
        }

    }
    suspend fun requestForDataSync(param: REQUEST,timeoutMillis: Long?=null):RESP{
        return if(timeoutMillis!=null) withTimeout(timeoutMillis) { getRepoCall(param) }
        else getRepoCall(param)
    }

    fun getCurrentAsyncRequest(): CompletableJob {
        return job
    }

    fun cancelRequest(){
        job.cancel()
    }
}

/**
 * UseCase(coroutines + flow for transferring data).
 * - It is similar to the other UseCase but it returns data as flow instead of using livedata.
 * - However it does not provide the functionality to automatically switch from concurrent thread to
 *   ui thread and relies on caller to do so
 * - some architectures provide flow **from the repo itself** for better concurrency.
 * - but here we only provide Flow in UseCase module. i.e, the whole operation of api call is going
 *   to happen at one go, and we will be getting the result as flow
 */
abstract class BaseConcurrencyFlowUseCase<REQUEST, RESP : Any> {
    private var currentExecutionDispatcher = Dispatchers.IO
    fun setExecutionDispatcher(dispatcher: CoroutineDispatcher){
        currentExecutionDispatcher = dispatcher
    }
    abstract suspend fun getRepoCall(param: REQUEST): RESP
    fun requestForDataAsFlow(param: REQUEST): Flow<RESP> {
        return flow { this.emit(getRepoCall(param)) }.flowOn(currentExecutionDispatcher)
    }
}


//example of a UseCase that uses coroutines and livedata for transferring data . checkout parent for more info
class GetAllUsersUseCase @Inject constructor(private val repo: UserRepo) :
    BaseConcurrencyUseCase<Unit, ApiResponse<List<UserResponse>>>() {
    override suspend fun getRepoCall(param: Unit): ApiResponse<List<UserResponse>> {
        return repo.getUserList()
    }
}

//example 2
class UpdateUserUseCase @Inject constructor( private val repo: UserRepo) :
    BaseConcurrencyUseCase<CreateUserRequest, ApiResponse<CreateUserResponse>>() {
    override suspend fun getRepoCall(param: CreateUserRequest): ApiResponse<CreateUserResponse> {
        return repo.updateUser(param)
    }
}

//example 3
class CreateUserUseCase @Inject constructor( private val repo: UserRepo) :
    BaseConcurrencyUseCase<CreateUserRequest, ApiResponse<CreateUserResponse>>() {
    override suspend fun getRepoCall(param: CreateUserRequest): ApiResponse<CreateUserResponse> {
        return repo.createUser(param)
    }
}

//example of a UseCase that uses coroutines + flow for transferring data . checkout parent for more info
class GetSingleUserUseCase @Inject constructor() : BaseConcurrencyFlowUseCase<Int, ApiResponse<JSONObject>>() {
    override suspend fun getRepoCall(param: Int): ApiResponse<JSONObject> {
        Log.e("TAG", "getRepoCall: called" )
        val json = JSONObject()
        val data = JSONArray()
        repeat(param * 10) {
            val obj = JSONObject()
            obj.put("id","${it+1}")
            obj.put("name", UUID.randomUUID()?.toString()?.replace("-", "")?.substring(0..8) ?: "12345678")
            data.put(obj)
        }
        json.put("data",data)
        return ApiResponse.Success(json)

    }
}


/**
 * ViewModel: The representation of lifecycle aware business logic
 *
 * A ViewModel is a class which do not gets destroyed when an activity is rotate, paused or resumed.
 *
 * - In a scalable architecture, ** its a 2 way communication module sending events from ui to other
 *   layers and data from other layers to ui**.  it is supposed to encapsulate all the static and
 *   dynamic data required by the ui , while at the same time, It is also supposed to have all the
 *   business logic (i.e calls to request for data) that should be executed on interaction
 *   with the views.
 * - it can also hold lifecycle aware components like Livedata and State(from compose),
 *   so that ui is only updated with the results when ui is alive.
 *
 */
@HiltViewModel
class UserViewModel @Inject  constructor(
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val getSingleUserUseCase: GetSingleUserUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
) : ViewModel() {

    /**
     * EXAMPLE 1
     *
     * **Basic** UseCase usage using **LiveData** in UseCase and **LiveData** for ViewModel/views.
     *
     * it is not recommended in UseCase,since livedata receives values only on the main thread and
     * api responses are not even logged when app is in background
     * */
    private val _allUsersLiveData = getAllUsersUseCase.responseLiveData
    val allUsersLiveData: LiveData<ApiResponse<List<UserResponse>>> = _allUsersLiveData
    fun getAllUsers(){
        _allUsersLiveData.value = (ApiResponse.Loading())
        getAllUsersUseCase.requestForData(Unit)
    }


    /**
     * EXAMPLE 1(b)
     *
     * A bit **Advanced** example of UseCase usage using **LiveData^** in UseCase and **LiveData**
     * for ViewModel/views.
     *
     * Actually we aren't using the Livedata of UseCase in this example. we are directly
     * accessing the repo call and handling the asynchronous behaviour in ViewModel only as we
     * need to make a transaction.
     *
     * More Details: Her we are calling the create user api for multiple times in parallel, so
     * that users are created in lesser time. but since we need to ensure that all users are
     * created and we send a succes response only after all users are created, we use a custom
     * extension of [asyncOrCancelAll] instead of [async]. check the extension for more info.
     * */
    private val _bulkCreateUsers =  MutableLiveData<ApiResponse<List<CreateUserResponse>>>()
    private val bulkCreateUsers: LiveData<ApiResponse<List<CreateUserResponse>>> = _bulkCreateUsers
    fun bulkCreate(users:List<CreateUserRequest>){
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val concurrentRequests: List<Deferred<ApiResponse<CreateUserResponse>>> = users.map {
                    asyncOrCancelAll(Dispatchers.IO) { createUserUseCase.requestForDataSync(it) }
                }
                val results: List<CreateUserResponse> = concurrentRequests.awaitAll().map {
                    if (it is ApiResponse.Success) it.data
                    else {
                        _bulkCreateUsers.value = ApiResponse.error("one or more request failed")
                        return@launch
                    }
                }
                _bulkCreateUsers.value = ApiResponse.success(results)
            }
            catch (t:Throwable){
                t.printStackTrace()
                _bulkCreateUsers.value = ApiResponse.error("something went wrong")
                return@launch
            }
        }
    }


    /**
     * A utility extension for coroutines that wraps over async extension, and allows the cancellation
     * of all the other async requests made in a particular coroutine scope if failed.
     *
     * this is helpful for transactional concurrent scenarios, where a task is split in x parts and
     * each part is executed in parallel, but we want to cancel the execution of all the parts
     * if execution of even 1 part fails.
     *
     * Eg: you want to upload 1 file by splitting into 5 parts, but if 1 of the parts fail to upload,
     * you would want to cancel uploading of all the parts. using [async] will continue uploading
     * the remaining parts, but using [asyncOrCancelAll] will cancel all the upload requests.
     *
     */
    fun <T> CoroutineScope.asyncOrCancelAll(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        //todo : move in coroutineextensions.kt
        return async(context,start) {
            try {
                if (isActive) block()
                else throw CancellationException("Task was cancelled")
            } catch (e: Exception) {
                coroutineContext.cancelChildren()
                throw e
            }
        }
    }




    /**
     * EXAMPLE 2
     *
     * UseCase usage using **Flow** in UseCase and **LiveData** for ViewModel/views.
     *
     * This is recommended for view based ui since flows are now emitted for UseCases
     * even though app is in background
     */
    private val _allObjectsLiveData =  MutableLiveData<ApiResponse<JSONObject>>()
    val allObjectsLiveData: LiveData<ApiResponse<JSONObject>> =  _allObjectsLiveData
    fun getAllObjects(millis:Int){
        viewModelScope.launch {
            getSingleUserUseCase.requestForDataAsFlow(millis).collect{
                _allObjectsLiveData.postValue(it)
            }
        }
    }




    /**
     * using flow in UseCase and state for ViewModel/views.
     * This is recommended for compose based architectures.
     */
//     private val _allObjectsState = mutableStateOf<JSONObject?>(value = null)
//     val allObjectsState: State<JSONObject?> =  _allObjectsState
//     fun getAllObjects2(millis:Int){
//          viewModelScope.launch {
//               getSingleUserUseCase.requestForDataAsFlow(millis).collect{
//                    _allObjectsState.value = it
//               }
//
//          }
//     }

}



data class CreateUserRequest(
    val name:String,
    val job:String,
    val id:Int? = null
)

data class CreateUserResponse(
    val name:String,
    val job:String,
    val id:String,
    val createdAt:String? = null,
    val updatedAt:String? = null
)
data class UserResponse(
    val avatar: String,
    val email: String,
    @SerializedName("first_name") val firstName: String,
    val id: Int,
    @SerializedName("last_name") val lastName: String
)


/**
 * Misc : missed topics:
 *  gradle caching/multi modular architecture etc.
 *  single module architecture (based on code feature)
 *  single module architecture (based on business logic)
 *  multi module architecture (based on code feature)
 *  multi module architecture (based on business logic)
 */




