#-*- coding: utf-8 -*-
import time
import telepot
from telepot.loop import MessageLoop

def handle(msg):
    content_type, chat_type, chat_id = telepot.glance(msg)

    if content_type == 'text':
        if msg['text'].upper() == 'STATUS':
            f = open("./simple_server_test/data/data.txt", 'r')
            data = f.read()
            bot.sendMessage(chat_id, data)
        elif msg['text'] == '/start':
            pass
        else:
            bot.sendMessage(chat_id, '지원하지 않는 기능입니다')


TOKEN = '675424900:AAH2XBMw2yvS6rytLzJtIQ5BMoJeL9lg_Gc'    # 텔레그램으로부터 받은 Bot API 토큰

bot = telepot.Bot(TOKEN)
MessageLoop(bot, handle).run_as_thread()
print ('Listening ...')

# Keep the program running.
while True:
    time.sleep(1000)
