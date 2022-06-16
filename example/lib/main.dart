import 'package:flutter/material.dart';
import 'package:flutter_twitter_login/flutter_twitter_login.dart';

void main() => runApp(const MyApp());

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  static final TwitterLogin twitterLogin = TwitterLogin(
    consumerKey: 'kkOvaF1Mowy4JTvCxKTV5O1WF',
    consumerSecret: 'ZECGsI6UUDBEUVGkJe4S5vd0FGqGxC3wMJCgsXgPRfjSwRFnyH',
  );

  String _message = 'Logged out.';

  void _login() async {
    final TwitterLoginResult result = await twitterLogin.authorize();
    String Message;

    switch (result.status) {
      case TwitterLoginStatus.loggedIn:
        Message = 'Logged in! username: ${result.session!.username}';
        break;
      case TwitterLoginStatus.cancelledByUser:
        Message = 'Login cancelled by user.';
        break;
      case TwitterLoginStatus.error:
        Message = 'Login error: ${result.errorMessage}';
        break;
    }

    setState(() {
      _message = Message;
    });
  }

  void _logout() async {
    await twitterLogin.logOut();

    setState(() {
      _message = 'Logged out.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Twitter login sample'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text(_message),
              RaisedButton(
                child: Text('Log in'),
                onPressed: _login,
              ),
              RaisedButton(
                child: Text('Log out'),
                onPressed: _logout,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
