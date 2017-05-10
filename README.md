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
override fun update(msg: Msg, model: Model): MC<Model, Msg> {
        return when (msg) {
            is Msg.Init -> ret(model)
            else -> {
                val (sm, sc) = update(msg, model.activity)
                ret(model.copy(activity = sm), sc)
            }
        }
    }
    
    fun update(msg: Msg.Fab, model:MFab ): MC<MFab, Msg> {
        return when (msg){
            is Msg.Fab.Clicked -> 
                ret(model.copy(clicked = msg.v),
                        Msg.Fab.ClickedDone(msg))
            is Msg.Fab.ClickedDone ->
                ret(model.copy(clicked = null))
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
class Greating(val greet:String)
val intialGreating = Greating("Hello")

// MODEL
data class Model (val activity : MActivity= MActivity())
data class MActivity (val greeting: Greating= intialGreating)

// MSG
sealed class Msg {
    object Init: Msg()
    sealed class Activity(): Msg(){
        class Greated(val v:Greating): Activity()
        class GreatedToggle : Activity()
    }
}

class ElmApp(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me) {
    override fun init(): MC<Model, Msg> { return ret(Model(), Msg.Init) }

    override fun update(msg: Msg, model: Model): MC<Model, Msg> {
        return when (msg) {
            is Msg.Init -> ret(model)
            is Msg.Activity -> {
                // several updates
                val (sm, sc) = update(msg, model.activity)

                ret(model.copy(activity= sm), sc)
            }
        }
    }

    fun update(msg: Msg.Activity, model: MActivity) :MC<MActivity, Msg>  {
        return when (msg) {
            is Msg.Activity.Greated -> {
                ret(model.copy(greeting = msg.v))
            }
            is Msg.Activity.GreatedToggle -> {
                val v = if (model.greeting==intialGreating) Greating("world") else intialGreating
                ret(model, Msg.Activity.Greated(v))
            }
        } 
    }

    /**
     * Render view, app delegates model review changes to children
     */
    override fun view(model: Model, pre: Model?) {
        val setup = { }
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    /**
     * Activity child setup the layout view. if has changes delegate render to child views
     */
    private fun  view(model: MActivity, pre: MActivity?) {
        val setup = { me.setContentView(R.layout.activity_example) }
        
        checkView(setup, model, pre) {
            view(model.greeting, pre?.greeting)
        }
    }

    private fun  view(model: Greating, pre: Greating?) {
        val setup = {
            val v = me.findViewById(R.id.greetingToggleButton) as ToggleButton
            v.setOnClickListener { v -> dispatch(Msg.Activity.GreatedToggle()) }
        }
        checkView(setup, model, pre) {
            val view=me.findViewById(R.id.greetingText) as TextView
            view.text = model.greet
        }
    }
}

class ExampleActivity : AppCompatActivity() {
    val app = ElmApp(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.mainLoop()
    }
}
```

#### Feedback is welcome
["Contact me @saffih "](https://twitter.com/saffih)
