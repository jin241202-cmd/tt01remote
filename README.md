# TT01 Remote

docomoテレビターミナル(TT01, Android TV 8.0)をWi-Fi経由で操作するAndroidアプリのソースプロジェクトです。
「Android TV Remote Protocol v2」(Google TVアプリが使っているのと同じプロトコル)でTT01と直接通信します。ADBや開発者向け設定は不要です。

## できること

- 同一Wi-Fi上のTT01とペアリング(初回のみ、TV画面に出るPINコードを入力)
- 電源・ホーム・戻る・十字キー・決定・音量・消音の送信

## できないこと / 注意点

- **Bluetoothリモコンとしての実装は含んでいません。** TT01純正リモコンはBLE HID(Bluetooth Low Energyの標準入力デバイスプロファイル)で、スマホ側でこれを「ペリフェラル(受け側)」として実装するのは機種依存が強く不安定なため、今回はより確実なWi-Fi方式のみ実装しています。
- ペアリング時の暗号処理(`pairingmessage.proto`, `PairingClient.kt`のsubmitPin内)は公開仕様からの再構築で、実機での検証はしていません。**もしPINを入力しても失敗する場合は、最初に疑うべきはここです。**下記「ビルド方法」の注記を参照してください。

## ビルド方法(Android Studio)

1. Android Studio(Hedgehog以降推奨)でこのフォルダ(`tt01remote/`)を「Open」で開く
2. 初回はGradle Syncが走ります。インターネット接続が必要です(依存ライブラリのダウンロードのため)
3. 実機のAndroidスマホをUSB接続するか、エミュレータを用意
4. 上部の実行ボタン(▶)でインストール・起動
5. apkファイルだけが欲しい場合: メニューの `Build > Build App Bundle(s) / APK(s) > Build APK(s)` を実行すると `app/build/outputs/apk/debug/app-debug.apk` が生成されます

## 使い方

1. TT01とスマホを同じWi-Fiに接続
2. TT01の「設定 > ネットワークとインターネット > Wi-Fi」でIPアドレスを確認(例: 192.168.1.42)
3. アプリを起動しIPアドレスを入力して「ペアリング開始」
4. TT01の画面に6桁のコードが表示されるので、アプリに入力
5. 成功すればリモコン画面に切り替わります

## ペアリングで詰まったら

`app/src/main/proto/androidtvremote2/pairingmessage.proto` の先頭コメントに書いた通り、この1ファイルだけは公開されている実装から確実な内容を直接取得できなかったため、私の再構築です。もし「TVがPINを表示するところまでは進むが、PINを入力すると失敗する」場合は:

1. https://github.com/tronikos/androidtvremote2/blob/main/src/androidtvremote2/pairingmessage.proto の内容をダウンロードして本ファイルと置き換える
2. `PairingClient.kt` の `submitPin()` 内のハッシュ計算(`clientCert.encoded + serverCert.encoded` の順でSHA-256)を、上記リポジトリの実装(Pythonの `pairing.py` 相当)と突き合わせて調整する

このあたりはネットワーク越しに実機で試行錯誤する必要があるため、Claude Code(ターミナル/デスクトップ版)でこのプロジェクトを開いて「TT01とのペアリングが失敗する、実機のログを見ながら直して」と頼むと、実際のTLS通信を見ながら調整できます。

## Bluetooth対応を追加したい場合

Android の BLE Peripheral(GATT Server)機能で HID over GATT を実装する必要があります。対応の可否は端末のBLEチップに依存し、Pixel等の一部機種でしか安定しません。実装のヒントは `BluetoothGattServer` + HID Report Map descriptor で検索してください。
