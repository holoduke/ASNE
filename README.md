<<<<<<< HEAD
MODIFIED VERSION OF ASNE 

Differences

* No need for having it instantiated inside a fragment. Can be made application wide available by adding it in your MainActivity
* Currently only working with facebook, google plus, vkontakte and twitter. Easy to fix the others

Code in MainActivity
```

    public SocialNetworkManager socialManager;

	public FacebookSocialNetwork fbNetwork;
	public TwitterSocialNetwork tnetwork;
	public VkSocialNetwork vnetwork;
	public GooglePlusSocialNetwork gnetwork;

	private void initSocialNetworks(){
		try {
			ArrayList<String> fbScope = new ArrayList<String>();
			fbScope.addAll(Arrays.asList("public_profile, email, user_friends"));
			SocialNetworkManager mSocialNetworkManager = (SocialNetworkManager) (getSupportFragmentManager().findFragmentByTag("sociallogin"));

			//hack because sometimes the social network gives null result when activty is restored from non saved instance
			if (mSocialNetworkManager != null) {
				try {
					mSocialNetworkManager.getSocialNetwork(FacebookSocialNetwork.ID);
				} catch (SocialNetworkException e) {
					mSocialNetworkManager = null;
				}
			}

			String[] vkScope = new String[0];

			Log.d(TAG, "create social manager");
			mSocialNetworkManager = new SocialNetworkManager();
			socialManager = mSocialNetworkManager;
			fbNetwork = new FacebookSocialNetwork(mSocialNetworkManager, this, fbScope);
			tnetwork = new TwitterSocialNetwork(mSocialNetworkManager, this, "{{key}}", "{{key2", "http://boogoo.android.app/");
			vnetwork = new VkSocialNetwork(mSocialNetworkManager, this, "{{key}}", vkScope);
			gnetwork = new GooglePlusSocialNetwork(mSocialNetworkManager, this);

			mSocialNetworkManager.addSocialNetwork(fbNetwork);
			mSocialNetworkManager.addSocialNetwork(tnetwork);
			mSocialNetworkManager.addSocialNetwork(vnetwork);
			mSocialNetworkManager.addSocialNetwork(gnetwork);
			getSupportFragmentManager().beginTransaction().add(mSocialNetworkManager, "sociallogin").commit();
		}
		catch(Exception e){
			Log.e(TAG,"error initializing social networks "+e.getMessage());
		}
	}

```


Implementation code would be something like:

```

//find existing logged in network type
final int logged_in_network = getActivity().getPreferences(0).getInt("social_network_id", -1);

SocialNetwork socialNetwork = null;
if (logged_in_network != -1) {
    Log.d(TAG,"found prev logged in network "+logged_in_network);
    socialNetwork = ((MainActivity) getActivity()).socialManager.getSocialNetwork(logged_in_network);
}

//if user is connected to an existing network
if (socialNetwork != null && logged_in_network != -1 && socialNetwork.isConnected()) {
    Log.d(TAG, "network is connected");
    try {
            socialNetwork.requestCurrentPerson(new OnRequestSocialPersonCompleteListener() {
                @Override
                public void onRequestSocialPersonSuccess(int i, SocialPerson socialPerson) {
                    Log.d(TAG, "on person request " + socialPerson.toString());

                    User u = new User();
                    u.username = socialPerson.name;
                    u.profileImage = socialPerson.avatarURL;
                    u.id = socialPerson.id;
                    u.networkid = logged_in_network + "";
                   
                    
                }

                @Override
                public void onError(int i, String s, String s2, Object o) {
                    Log.e(TAG, "error " + s + "---" + s2);
                    Toast.makeText(getActivity(), "ERROR: " + s, Toast.LENGTH_LONG).show();
                }
            });
    
    } catch (Exception e) {
        Log.e(TAG, "error get person " + e.getMessage());
    }
//if user is not logged in
} else {

    //Usualy you want to add a dialog fragment here with a list of social networks.
    //After clicking a social network you would get a networkid
    
    final SocialNetwork socialNetwork = ((MainActivity) getActivity()).socialManager.getSocialNetwork(networkid);
    
    //cancel all previous operations
    socialNetwork.cancelAll();
    
    Log.d(TAG, "request login networkid: " + networkid);
         
    socialNetwork.requestLogin(new OnLoginCompleteListener() {
        @Override
        public void onLoginSuccess(final int networkid) {
            Log.d(TAG, " login succes");

            socialNetwork.requestCurrentPerson(new OnRequestSocialPersonCompleteListener() {
                @Override
                public void onRequestSocialPersonSuccess(int i, SocialPerson socialPerson) {


                    Log.d(TAG, "on account receive success" + socialPerson.toString());
                    User u = new User();
                    u.username = socialPerson.name;
                    u.id = socialPerson.id;
                    u.networkid = networkid + "";
                    u.profileImage = socialPerson.avatarURL;
                }

                @Override
                public void onError(int i, String s, String s2, Object o) {
                    Log.e(TAG, "error " + s + "---" + s2);
                    Toast.makeText(getActivity(), "on account receive error: " + s, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onError(int i, String s, String s2, Object o) {
            try {
                Log.e(TAG, " login error " + s + "---" + s2);
                Toast.makeText(getActivity(), "Login error: " + s, Toast.LENGTH_LONG).show();
            }
            catch(Exception e){

            }
            newFragment.dismissAllowingStateLoss();
        }
    });
}
           

```




=======
Unfreeze project - merge all puls for master branch - current version was not tested - **NOT STABLE**

**Sorry, but I freeze work on project for some time(I hope not long, nearly month) - too busy. I saw all Issues and will resolve them as soon as I can.** 
>>>>>>> 91bf48d9fc2560400bc6210eb95a8eb68a76920f


![enter image description here][1]

ASNE        [![ASNE Maven Central](http://img.shields.io/badge/ASNE%20Maven%20Central-0.3.1-brightgreen.svg?style=flat)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.asne%22) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-ASNE-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/868)
=====================
ASNE library created for simple integration of social setworks to android applications. If you want to integrate your application with multiple social networks just choose ASNE modules and add them to your project. You just need to: add module, build SocialNetworkManager and configure your AndroidManiferst. 
ASNE contains common interface for most popular social networks, but you can easily make module for another.

ASNE contains modules for social networks:
 - Twitter  
 - LinkedIn  
 - Facebook 
 - Google Plus 
 - Instagram
 - Vkontakte 
 - Odnoklassniki

Table of contents
-----------
[Features](https://github.com/gorbin/ASNE/blob/master/README.md#features)  
[Documentation](https://github.com/gorbin/ASNE/blob/master/README.md#documentation)  
[Demo app](https://github.com/gorbin/ASNE/blob/master/README.md#demo-app)  
[Getting started](https://github.com/gorbin/ASNE/blob/master/README.md#getting-started)  
[Important](https://github.com/gorbin/ASNE/blob/master/README.md#important)  
[Developed By](https://github.com/gorbin/ASNE/blob/master/README.md#developed-by)  
[License](https://github.com/gorbin/ASNE/blob/master/README.md#license)  

Features
-----------
ASNE got [almost all necessary requests](https://github.com/gorbin/ASNE/wiki/%D0%A1%D0%BF%D0%B8%D1%81%D0%BE%D0%BA-%D0%BC%D0%B5%D1%82%D0%BE%D0%B4%D0%BE%D0%B2-SocialNetwork) to social networks

 - Login
 - Configure necessary permissions
 - Get Access Token
 - Get current person social profile
 - Get social profile of user by id
 - Get social profile for array of users 
 - Get detailed user profile
 - Share message
 - Share photo
 - Share link
 - Request Share dialog with message/photo/link
 - Check is user(by id) is friend of current
 - Get list of Friends
 - Adding friends by id
 - Remove friend from friend list
 - Any request to chosen social network - you got full SDK/API
 
![enter image description here][3]

Documentation
-----------
[Javadoc](http://gorbin.github.io/ASNE/)

[**Tutorial project on GitHub**](https://github.com/gorbin/ASNETutorial)

[**Tutorial article on CodeProject**](http://www.codeproject.com/Articles/815900/Android-social-network-integration)

Demo app
====
[Link for releases][4]

<a href="https://play.google.com/store/apps/details?id=com.gorbin.androidsocialnetworksextended.asne">
  <img alt="Get it on Google Play"
       src="https://developer.android.com//images/brand/ru_generic_rgb_wo_60.png" />
</a>

Getting started
=====================

**Adding library**

_1) Using Maven Central_

Add dependency for chosen module, here example for all modules, you can choose one or two

```
dependencies {
...
    compile 'com.github.asne:asne-facebook:0.3.1'
    compile 'com.github.asne:asne-twitter:0.3.1'
    compile 'com.github.asne:asne-googleplus:0.3.1'
    compile 'com.github.asne:asne-linkedin:0.3.1'
    compile 'com.github.asne:asne-instagram:0.3.1'
    compile 'com.github.asne:asne-vk:0.3.1'
    compile 'com.github.asne:asne-odnoklassniki:0.3.1'
...
}
```

_2) Import module to your project_

For example, in AndroidStudio you can add modules via Gradle: 

 1. Copy social module to your project.
 2. In settings.gradle include `':ASNECore', ':socialNetworkModuleName'`
 3. In build.gradle of your app (YOUR_PROJECT/app/build.gradle) add new dependencies: `compile project(':socialNetworkModuleName') `

Without Gradle, add ASNE like: 
 1. Open Project Settings and choose Modules. 
 2. Find button "Add" (+), and choose Import module 
 3. Find ASNECore and socialNetworkModuleName directories - «Add». 
 4. Choose Create module from existing sources, then click "Next" rename module from "main" to "ASNECore". 
 5. Add new asne-module in dependencies to your app. 

**Using library**

Firstly, you need to create app in social network. You can read about main steps:

 - [Twitter](https://github.com/gorbin/ASNE/wiki/Create-Twitter-App)
 - [LinkedIn](https://github.com/gorbin/ASNE/wiki/Create-LinkedIn-App)
 - [Facebook](https://github.com/gorbin/ASNE/wiki/Create-Facebook-App)
 - [Google Plus](https://github.com/gorbin/ASNE/wiki/Create-Google-Plus-app) 
 - [Instagram](https://github.com/gorbin/ASNE/wiki/Create-Instagram-App)
 - [Vkontakte](https://github.com/gorbin/ASNE/wiki/Create-Vkontakte-App) 
 - [Odnoklassniki](https://github.com/gorbin/ASNE/wiki/Create-Odnoklassniki-App)

Second, we need to catch response after login via social network login dialog:

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Fragment fragment = getSupportFragmentManager().findFragmentByTag(BaseDemoFragment.SOCIAL_NETWORK_TAG);
    if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
```

Then, you need to initialize `mSocialNetworkManager`, it contain common interface for all ASNE social network modules. Initialize chosen social network and add social network to SocialNetworkManager(example: FacebookSocialNetwork):

```java
mSocialNetworkManager = (SocialNetworkManager) getFragmentManager().findFragmentByTag(SOCIAL_NETWORK_TAG);
	if (mSocialNetworkManager == null) {
        mSocialNetworkManager = new SocialNetworkManager();
		FacebookSocialNetwork fbNetwork = new FacebookSocialNetwork(this, fbScope);
		//or from an activity
		//FacebookSocialNetwork fbNetwork = new FacebookSocialNetwork(mSocialNetworkManager, this, fbScope);
        mSocialNetworkManager.addSocialNetwork(fbNetwork);
        getFragmentManager().beginTransaction().add(mSocialNetworkManager, SOCIAL_NETWORK_TAG).commit();
	}
```    
     
where `fbScope` is **permissions** for your app, for example I used:

```java
ArrayList<String> fbScope = new ArrayList<String>();
fbScope.addAll(Arrays.asList("public_profile, email, user_friends, user_location, user_birthday"));
```

 Then you can send requests to social network like:

 ```java
	mSocialNetworkManager.getVKSocialNetwork().requestLogin(new OnLoginCompleteListener() {
        @Override
        public void onLoginSuccess(int socialNetworkID) {

        }

        @Override
        public void onError(int socialNetworkID, String requestID, String errorMessage, Object data) {

        }
    });
```

Or get Social network directly like:

```java
	Session session = Session.getActiveSession();
```

Important
=====================

**Facebook Upgrades**

Facebook some permissions you can get only after Facebook submission, so my demo app wasn't submitted due low functionality. So if you want to use it with all functionality send me your facebook id and I add you as tester - this is easy way to to fully use demo app
email: gorbin.e.o@gmail.com

Apps are no longer able to retrieve the full list of a user's friends (only those friends who have specifically authorized your app using the user_friends permission) but if you add me as friend you will see me in friendlist([profile][6])

Developed By
=====================
ASNE developed on the basis of ([Android Social Networks][2]) mostly redone and add new features(some features are pulled to Android Social Networks)

Evgeny Gorbin - <gorbin.e.o@gmail.com>

<a href="https://plus.google.com/108381948947848082245">
  <img alt="Follow me on Google+"
       src="https://raw.githubusercontent.com/gorbin/ASNE/master/resources/gp.png" />
</a>
<a href="https://twitter.com/egorbin">
  <img alt="Follow me on Twitter"
       src="https://raw.githubusercontent.com/gorbin/ASNE/master/resources/twitter.png" />
</a>
License
=====================
ASNE is made available under the MIT license: [MIT license](http://opensource.org/licenses/MIT):

<pre>
The MIT License (MIT)

Copyright (c) 2014 Evgrny Gorbin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
</pre>

  [1]: https://raw.githubusercontent.com/gorbin/ASNE/master/resources/recomended.png
  [2]: https://github.com/gorbin/AndroidSocialNetworks
  [3]: https://raw.githubusercontent.com/gorbin/ASNE/master/resources/main.png
  [4]: https://github.com/gorbin/ASNE/releases
  [5]: https://github.com/gorbin/ASNE/releases/download/0.2/ASNE-debug-unaligned.apk
  [6]: https://www.facebook.com/evgeny.gorbin
