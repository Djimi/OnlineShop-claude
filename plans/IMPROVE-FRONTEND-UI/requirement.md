First kill of ui apps (there are lefovers I thikg 5173 and 5174 ports)

After that:
I would like to modernize and improve the UI of frontend app a bit:
- theer are "create account" and "register"which is the same
- there are "login" and "log in" which are the same
- buttons are too long - take the whole screen. make them universal size 5-6 cm (or whatever is normal)
- be sure sign out is only available if you are logged in 
- some example of nice UI is : https://cdn.dribbble.com/userupload/17299496/file/original-44214a4ccaa445d1b78f0b89935c9340.png?resize=1905x1429&vertical=center. Follow these colors also
- in case the backend cannot answer within 5 seconds show red popup message that an networ kerror occured
- do similar checks for the modern view of the items when you are logged - does items seems good ordered, etc.
- do the same for the initial page
- one button should appear only once in a page - so only one Register for example.

Do not stop until the final page is pretty, ready and working. Test whether it is working for sign in, register, etc

Once you think you are done check whether all conditions are met, if not loop again.

Keep in mind that you are in devcontainer and the infrastrcuture is set for you - redis, postgres, etc. If there are issue with them, stop and tell.