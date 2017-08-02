# ElmDroid
Elm style Init/Update/View for the droid.

# Extended 
- The maps example was extended with Gps service by Messenger.
- The Hello world example has additional broadcast listener accepting incoming sms. 
- Use of delegation into sub module. Hello Example use a "child" pattern delegate to Turtle.

`Inspired by:` 
- [Elm](http://elm-lang.org/)
- [Kotlin](https://kotlinlang.org/)
- [@lungos POC](https://github.com/glung/elm-architecture-android) 
- [Redux](http://redux.js.org/docs/introduction/PriorArt.html)


## Style
Elm like structue. 
Msg for all event/message passing.
Model holding the (immutable) state of the system.
methods for update and view.

### Msg
Use Kotlin strong typing by a hierarchy of sealed classes and objects.
```kotlin
sealed class Msg {
    object Init : Msg()
    
    sealed class Option : Msg() {
        class Navigation(val item: NavOption) : Option()
        class ItemSelected(val item: ItemOption) : Option()
        class Drawer(val item: DrawerOption  = DrawerOption.opened) : Option()
    }
    // ...
}

```


### Model
Have the model and the Msg match and use same class types `NavOptions` `DrawerOptions`.

### update
The message `msg` propogated update method updating (creating a new) state, 
and issue additional events (Msg commands) if needed.
The pattern used is several methods called update with proper Msg and Model parameters
the code `return when(msg)` with code block matching the msg type. 
The code return the new state for the sub model and optional Msg command.

```kotlin
override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> model
            else -> {
                val m = update(msg, model.activity)
                model.copy(activity = m)
            }
        }
    }
    
    fun update(msg: Msg.Fab, model:MFab ): MFab {
        return when (msg){
            is Msg.Fab.Clicked -> {
                dispatch(Msg.Fab.ClickedDone(msg))
                model.copy(clicked = msg.v)
            }
            is Msg.Fab.ClickedDone ->
                model.copy(clicked = null)
        }
    }
```

#### Helper ret
A helper method for returning new model (and optional Msg)
 
### view
Since the view in android has a state the method signature is different.
There is a certain pattern to use. It MUST do identity check `===` and return if there is no change.
Usually check for null for initial "view" creation and listener binding, only then we delegate to child sub models or or update the view with the state.
```kotlin
 override fun view(model: Model, pre:Model?) {
        checkView( {} , model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = { me.setContentView(R.layout.nav_activity_example2) }
        checkView( setup , model, pre) {
            view(model.toolbar, pre?.toolbar)
            view(model.fab, pre?.fab)
            view(model.options, pre?.options)
        }
    }
 
```

#### Helper checkView
A helper method that check for the changes and if there were it calls the render block, if the model was null it would call setup

### Event listeners - android UI 
Event listeners should `dispatch` the Msg event (with appropriate properties/payload), 
which subsequently update the model (i.e. onClick and listeners invoked) 
 
dispatching of messages from within the Activity is done with `dispatch`
### External events  
In cases we need to interact from the outside (`timer`...) use `postDispatch`



## HelloWorld

```kotlin
// POJO
class Greeting(val greet: String)

val intialGreating = Greeting("Hello")

// MODEL
data class Model(val activity: MActivity = MActivity())
data class MActivity(val greeting: Greeting = intialGreating)

// MSG
sealed class Msg {
    object Init : Msg()
    sealed class Activity : Msg() {
        class Greeted(val v: Greeting) : Activity()
        class GreetedToggle : Activity()
    }
}


class ElmApp(override val me: HelloWorldActivity) : ElmBase<Model, Msg>(me) {
    override fun init(): Model {
        dispatch(Msg.Init)
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> { // init delegation to components...
                model
            }
            is Msg.Activity -> { 
                val m = update(msg, model.activity)
                model.copy(activity = m)
            }
            // other group updates if had.  
        }
    }

    fun update(msg: Msg.Activity, model: MActivity): MActivity {
        return when (msg) {
            is Msg.Activity.Greeted -> {
                model.copy(greeting = msg.v)
            }
            is Msg.Activity.GreetedToggle -> {
                val v = if (model.greeting == intialGreating) Greeting("world") else intialGreating
                // simpler code which does not show the use of dispatch from within an update
                // would have been:
                //     model.copy(greeting = v)
                // but instead we do a dispatch that would be hanled later by the when branch above.
                dispatch(Msg.Activity.Greeted(v))
                model
            }
        }
    }


    /**
     * Render view, app delegates myModel review changes to children
     */
    override fun view(model: Model, pre: Model?) {
        val setup = { }
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    /**
     * Activity impl setup the layout view. if has changes delegate render to impl views
     */
    private fun view(model: MActivity, pre: MActivity?) {
        val setup = { me.setContentView(R.layout.activity_helloworld) }

        checkView(setup, model, pre) {
            view(model.greeting, pre?.greeting)
        }
    }

    private fun view(model: Greeting, pre: Greeting?) {
        val setup = {
            me.greetingToggleButton.setOnClickListener { 
                v -> dispatch(Msg.Activity.GreetedToggle()) 
            }
        }
        checkView(setup, model, pre) {
            me.greetingText.text.clear()
            me.greetingText.text.append(model.greet)
        }
    }
}

class HelloWorldActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.onCreate()
    }

    override fun onDestroy() {
        app.onDestroy()
        super.onDestroy()
    }
}
```

#### Feedback is welcome
["Contact me @saffih "](https://twitter.com/saffih)
