
# IJTools_WebAPItoRunMacro

[日本語の説明 - >](#日本語)  
[The english discriptions - >](#English)

## 日本語

ImageJをWEB API化するプラグイン「IJTools_WebAPItoRunMacro」を自作しました。と言っても、マクロを実行するだけのサンプル的なプラグインです。

## 目的

* API名「run_macro」の機能は、ImageJマクロを実行することです。
  * [GET] `/run_macro?macro=hoge&par1=1&par2=2`
* API名「post_image」の機能により、画像をImageJに転送します。
  * [POST] `/post_image?name=hoge`

## ビルド方法

NetBeansでビルドできます。[こちら](https://waku-take-a.github.io/NetBeans%25E3%2581%25AB%25E3%2582%2588%25E3%2582%258BPlugin%25E4%25BD%259C%25E6%2588%2590.html)のサイトを参考にしてください。

## バイナリー

作成したプラグインのバイナリー(jarファイル)を[こちら](https://github.com/WAKU-TAKE-A/IJTools_WebAPItoRunMacro/releases)に置いておきます。<br>是非使ってみてください。

## English

I created a plug-in "IJTools_WebAPItoRunMacro" that turns ImageJ into a WEB API. That said, it's just a sample plugin that runs macros.

## Purpose

* The function of the API name "run_macro" is to execute the ImageJ macro.
  * [GET] `/run_macro?macro=hoge&par1=1&par2=2`
* The function of API name "post_image" transfers the image to ImageJ.
  * [POST] `/post_image?name=hoge`

## How to build

The files can be built with NetBeans. Please refer to [this site](https://waku-take-a.github.io/How%2520to%2520use%2520OpenCV%2520from%2520ImageJ.html).

## Binaries

There are the binaries (jar files) of the created plugins in [here](https://github.com/WAKU-TAKE-A/IJTools_WebAPItoRunMacro/releases). Please try using it.
