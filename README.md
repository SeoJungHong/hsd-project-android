# hsd-project-android
SNU 2016-1 Hardware System Design Project

Arduino, Android, FPGA 를 이용한 스마트 워치 만들기 프로젝트 중 Android 코드. Android app 은 Bluetooth 를 통해 FPGA 에 연결하여 Arduino 와 통신한다.

## Done
- Connect to Bluetooth using Service
- Custom Features
    - Time : 시계 기능
    - Heart : Arduino 센서를 통해 측정한 심박수 정보가 있을 경우 Pulse 및 BPM 을 표시한다.
    - Weather : 현재 위치를 수집해 날씨 정보를 보여준다.
    - Music : 음악 플레이어. 기기에 저장된 media 를 불러와서 음악 리스트 구현

## TODO
- Design
- Upgrade Music player
