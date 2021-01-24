

from tweepy import Stream
from tweepy import OAuthHandler
from tweepy.streaming import StreamListener
import json
import urllib
from urllib import request
from bs4 import BeautifulSoup
import os
import sys


# Stream data in real time from public tweets
class TwitterListener(StreamListener):
    # overwrite the initializing function from StreamListeners to add a counter to count # of tweets streamed
    def __init__(self, api=None):
        super(TwitterListener, self).__init__()
        self.numTweets = 0  # initilize tweet count to 0

    # get data real time twitter stream
    def on_data(self, rawData):
        self.numTweets += 1  # increment counter each time a tweet is Streamed
        #global fileOutput
        global fileName
        global fileCnt
        global f
        #global append_write

        if fileCnt < numFiles: #Stop program once fileNum files are created
            #check if file exists, create it if it does not
            if os.path.exists(fileName):
                append_write = 'a' # append if already exists
            else:
                append_write = 'w' # make a new file if not
            f = open(fileName, append_write, encoding="utf8", newline='')

            #if self.numTweets <= tweetsCollect: #tweets_to_collect:
            if os.stat(fileName).st_size <= 1.0e6: # If file size exceeds 1 MB
            #if self.numTweets <= tweetsCollect:
            #if os.stat(fileName).st_size <= 1.0e7: # If file size exceeds 10 MB
                # add tweet json data to file stops after 10 MB of tweets
                #with open(fileName, append_write, encoding="utf8", newline='') as f:
                f.write(rawData)
                print("\tCollecting tweet %d." % (self.numTweets), end='\r')
                #self.numTweets += 1
                return True
            else:
                f.close()
                fileCnt += 1
                newFileNumber = str(fileCnt)
                fileName = "data" + newFileNumber + ".txt"
                print("")
                #return False
        else:
            # Reset some variables for collectTweets()
            #fileName = "data0.txt" #This declaration doesn't go to processTweets()
            #fileCnt -= 1 #Not needed if range() goes from 0 to fileCnt-1
            return False

    # print error code if an error occurs in during stream
    def on_error(self, status):
        print("Error code:", status)


# function collects twitter Json data into a file, each line is a separate json
def collectTweets():
    apiKey = "Q4TOrXgAlcSrOptneddWCk3SV"  # API key
    apiSecret = "V0Aso4J7r3oV2apNEeXXE4bAmbsRFfJEdLC9eFgYQ1Vqaiz73e"  # API secret key
    aToken = "752734942744895488-hH4bPKQZk5BrnYw9gj4qsBUkMPOHuLA"  # Access token
    aSecret = "9fNPE2652lxdWNc3TN7kpBm5PlDDRmft4h6zTKLlynNNa"  # Access secret token

    auth = OAuthHandler(apiKey, apiSecret)  # set authorization key
    auth.set_access_token(aToken, aSecret)  # set access token

    twitterStream = Stream(auth, TwitterListener())  # start tweet stream
    twitterStream.filter(languages=['en'], locations=[-72.6836,-37.6629,-72.039,-37.1785], 
                        track=['#', '#covid19', '#coronavirus', '#covid', '#corona', '#georgefloyd', '#blm', '#blacklivesmatter'],
                          is_async=False)
    #twitterStream.filter(languages=['en'], locations=[-117.6763,33.4259,-114.4349,34.0801], is_async=False)

# Process data from file containing twitter json data
def processTweets():
    tweets = {}
    rejoinedTweet = ' '  # string to rejoin tweets to remove potential newlines
    i = 1
    temp = ""
    title = ""
    titleStr = ""
    jsonTitle = ""
    fileName = "data0.txt"
    
    outputFileName = "output.txt" #This will output the parsed JSON objects from the data files
    #check if file exists, create it if it does not
    if os.path.exists(outputFileName):
        append_write = 'a' # append if already exists
    else:
        append_write = 'w' # make a new file if not
    outputFile = open(outputFileName, append_write, encoding="utf8", newline='')

    print("FileCnt is", fileCnt)
    for x in range(fileCnt):
        print("top of for loop")
        f = open(fileName, 'r')
        line = f.readline()
        while line:
            #print("Top of the while line:")
            try:
                twitterData = json.loads(line)  # get entire JSON object of a tweet
                username = twitterData["user"]["screen_name"]  # get users screen name
                tweet = twitterData["text"]  # get text body  from a tweet
                URL = twitterData["entities"]["urls"]
                date = twitterData["created_at"]
                index = 0
                hashtags = [""]*len(twitterData["entities"]["hashtags"])           #Initialize with number of hashtags
            
                #print("Num hashtags = ", len(twitterData["entities"]["hashtags"]))
                for index in range(len(twitterData["entities"]["hashtags"])):
                    hashtags[index] = twitterData["entities"]["hashtags"][index]      

                print("")
                print("------------------------- tweet ", i, " -------------------------")
                #print("Num hashtags = ", len(twitterData["entities"]["hashtags"]))
                location = twitterData["user"]["location"]

                # loop through all urls in a users tweet
                for url in twitterData["entities"]["urls"]:
                    #print("PRINTING url[expanded_url] for tweet", i)
                    #print(url["expanded_url"])  # print full url for testing, remove later
                    print("")
                    url_link = url["expanded_url"]  # get url link for request

                    try:
                        # get the webpage html
                        html = request.urlopen(url_link)\
                            .read()\
                            .decode('utf8') 
                        # Get webpage title
                        soup = BeautifulSoup(html, 'html.parser')
                        title = soup.find('title')
                        if title is not "null":
                            titleStr = title.string
                            # Split to remove new lines
                            temp = titleStr.split()
                            jsonTitle = rejoinedTweet.join(temp)

                    except urllib.error.URLError:
                        # error if url could not be connected to
                        print("URL Could Not Connect")
                    except TimeoutError:
                        # error if url request times out
                        print("URL Request Time Out")
                    except AttributeError:
                        print("AttributeError")
                    except ConnectionResetError:
                        print("Connection Reset Error")
                    except Exception:
                        print("Error occured")

                #print("***************************")
                #print("jsonTitle = ", jsonTitle)
                #print("***************************")
                if jsonTitle is not "":
                    #Add the title to the json object
                    twitterData["entities"]["urls"].append({'page_title': jsonTitle})
                    #reset the web page title
                    #jsonTitle = ""

                #print(json.dumps(twitterData, indent=4, sort_keys=True))

                tweets[i] = (username, tweet, URL)
            
                print("Username: ", username)
                print("Tweet: ", tweet)
                print("URLs: ", URL)
                print("Page Tile:", jsonTitle)
                print("Date: ", date)
                print("Location: ", location)
                print("Hashtags: ", hashtags)
                print("")

                outputFile.write("UsernameHandle " + username + " ")
                newTweet = tweet.replace('\n',' ')
                outputFile.write("TweetContents " + newTweet + " ")
                if jsonTitle is "":
                    outputFile.write("PageTitle None ")
                else:
                    outputFile.write("PageTitle " + jsonTitle + " ")
                outputFile.write("UserPostDate " + date + " ")
                if location:
                    outputFile.write("UserLocation " + location + " ")
                else:
                    outputFile.write("UserLocation " + "None ")
                outputFile.write("UserHashtags ")
                if(len(hashtags) is 0):
                    outputFile.write("None")
                singleHashtag = ""
                for tag in range(len(hashtags)):
                    singleHashtag = hashtags[tag]["text"]
                    outputFile.write(singleHashtag + " ")
                outputFile.write("\n")
            
                jsonTitle = "" #Reset the page title
            
                line = f.readline()  # read next line from file
                i += 1
            except Exception:
                print("Error")
                break;

        #end while
        f.close()
        newFileNum = str(x)
        fileName = "data" + newFileNum + ".txt"

    outputFile.close()

    return tweets


if __name__ == '__main__':
    #fileOutput = "data"
    fileName = "data0.txt"  # Store the json data
    fileCnt = 0 #Number of files created by program... Initialize to 0
    numFiles = int(sys.argv[1]) #Desired number of files to create / write to
    #print("FILENUM = ", numFiles)
    tweetsCollect = 3;

    #myStr = "Hello there guys"
    #newStr = myStr.replace(" ", '').replace(" ", '')
    #print(newStr)

    print("Collecting tweets now.")
    collectTweets()
    print("Tweet collection complete.\n")


    print("Processing tweets now.")
    tweets = processTweets()
    print("Tweet processing complete")





