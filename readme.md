# ![GPTGodIcon](https://github.com/user-attachments/assets/15ee2068-82d8-419a-9247-17332ec84600) GPTGOD Bukkit Plugin
Modified version of the [ChatGPT bukkit plugin by "BigYous"](https://github.com/YOUSY0US3F/minecraft-gpt-god-plugin) which uses the google gemini (within free-tier limits) to provide a similar AI roleplay experience free of charge.

What's Changed:

- [x] Ported to google gemini from chatgpt
    - [ ] context window is now one million for `gemini-1.5-flash` and 2 million for `gemini-1.5-pro`
    - [x] Voice transcription requests are now bundled (to avoid api spam) and using the gemini 1.5 flash model
      - [x] scrapped all openai support at the moment in favour of full gemini consistency
- [x] Ported voice synthesis to Speechify (super godly narrator voice for free!)
- [x] Improved context handling (cured the AI god of dementia)
- [x] seperated model usage into "primary" and "secondary" model so that a cheaper model may be used for medial tasks
- [x] decreased call count to primary model and unified commands and communication behaviour to one context 
- [x] Added real multi-turn mode (including model responses in context)
- [x] Improved system instruction prompt design for better roleplay output
- [x] Gave the AI a better sense of time using minecraft time of day timestamps
- [x] various bug fixes and codbase improvements
- [x] added new decree function for the ai to drop floating commandments in the world if it is displeased
- [x] added model tempurature configuration control to improve model creativity
- [x] Rolling context expiry for new multi-turn when token limit is approaching

Todo:

- [ ] Maybe add option to choose between gemini and open ai?
- [ ] Fix bug with events coming in after round restart
- [ ] Give the AI eyes using the papermc ImageryAPI and gemini vison api (so the AI can decide if your monuments to its honor are ugly or not)

## Local Setup

- clone the repo
- run `./gradlew` in the root of the repo
- download [paper mc server version 1.20.4](https://papermc.io/downloads/paper)
- setup the server
- download [voicechat bukkit plugin 2.5.1](https://modrinth.com/plugin/simple-voice-chat/version/bukkit-2.5.1)
- place the jar in the plugins folder of your server
- [install fabric for 1.20.4](https://fabricmc.net/use/installer/)
- download [the voice chat mod version 1.20.4-2.5.4 for fabric](https://modrinth.com/plugin/simple-voice-chat/version/fabric-1.20.4-2.5.4)
- place that in your mods folder in .minecraft

## Config
- Get a free gemini api key from [aistudio.google.com/](https://aistudio.google.com/)
- Get a free speechify api key from [console.sws.speechify.com/tts](https://console.sws.speechify.com/tts)
- configure primary model to be `model-name: gemini-1.5-flash` if you want fast model times such as `rate: 30` or below.
    - Note: Do not go below 30-32 on the free tier pro model or you will hit your 2 requests per minute and 50 requests per day fast
    - the flash model can go below 20 and performs quite nicely with the rapid feedback
    - for best experience try setting both models to `gemini-1.5-flash` and try a rate of about `rate: 20` to `rate: 30`
    - do NOT use `gemini-1.5-pro` as the secondary model

## Building

- use the shadowjar task to build
- the jar will appear in build/libs
- place this jar in the plugins folder of the server

## Dependencies

to add a dependency add it like this:

``` Groovy

dependencies {
    implementation 'com.google.code.gson:gson:2.10.1'
}

shadowJar {
    dependencies {
        include(dependency('com.google.code.gson:.*'))
    }
}

```

## Running

- go to `plugins/gptgodmc/config.yml`
- paste in your OpenAi API or Gemini key
- run the server
- launch minecraft with fabric
- connect to the server at `localhost`
