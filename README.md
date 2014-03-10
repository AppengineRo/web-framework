This is a framework for an App Engine Application

For Datastore callbacks you must:
    1) extend AbstractDatastoreCallbacks
    2) set in appengine.xml the full name of the class in a system property
        <system-properties>
            <property name="datastoreCallbacksClass" value="com.example.DatastoreCallbacks"/>
        </system-properties>
Please use Datastore if you want the datastore callbacks to work

If you use federated login with Yahoo authentication then you must insert in web.xml:
(You will see requests to /_ah/xrds in logs)
    <servlet>
        <!--
        http://jeremiahlee.com/blog/2009/09/28/how-to-setup-openid-with-google-apps/
        http://en.wikipedia.org/wiki/XRDS
        https://developers.google.com/identity-toolkit/v2/devconsole
        http://stackoverflow.com/questions/7529013/aol-openid-website-verification
        Pe scurt yahoo + inca catva vor sa verifice ceva legat de OpenID
        -->
        <servlet-name>xrds</servlet-name>
        <servlet-class>ro.adma.Xrds</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>xrds</servlet-name>
        <url-pattern>/_ah/xrds</url-pattern>
    </servlet-mapping>

