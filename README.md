# Fair-Verifiable-Secret-Sharing源代码运行说明

## 1.链下环境

（1）Java Development Kit 8；

（2）Apache Maven 3.9.3；

（3）使用的库类：Bouncycastle库、JPBC库。

## 2.区块链环境

（1）Hyperledger Fabric 2.3；

（2）Docker 24.0.2；

（3）go 1.21.4。

## 3.目录结构

（1）**src**：源码；

对源码中主要类进行解释说明：

①elgamal：Elgamal加密算法类；

②FVSS：公平可验证性秘密共享算法类；

③ParticipantsContract：公平可验证性秘密共享链码；

④Main：FVSS类中各成员函数对应的主函数测试；

⑤RSACryptoExample：RSA加密算法类；

⑥ParticipantQueryResultList：以列表形式存储节点的查询结果；

⑦Participants：节点类；

⑧ParticipantsQueryResult：存储节点的查询结果；

⑨putShadowShares：存储影子秘密份额；

⑩UserContract：用户链码；

（2）**test**：测试类；

对主要测试类进行解释说明：

①createParticipantTest：对链码中的createParticipants()函数进行单元测试；

②delParticipantTest：对链码中的delparticipants()函数进行单元测试；

③initLedgerTest：对链码中的初始化账本方法initLedger()进行单元测试；

④queryParticipantsTest：对链码中的queryParticipant()函数进行单元测试；

⑤MaliPeerVerifyTest：模拟恶意分发方的恶意行为，对FVSS中的份额验证算法进行单元测试。

（3）**pom.xml**：相关库类管理。

## 4.源码运行说明：

（1）链下运行源码说明

建议按照“**1 链下环境**”中对应版本号预先配置好JDK、Java、Maven等实验环境，再在IntelliJ IDEA 2022.2.3（版本号可选）中运行源码；

（2）链码打包、安装、部署等说明：

①建议按照“**2.区块链环境**”中对应版本号预先配置好区块链环境；

②由于我的区块链环境以及链码安装都在虚拟机里进行，所以我会预先安装好**Ubuntu 20.04**（使用其他版本的虚拟机或服务器也可以！）；

③由于我的链码使用的开发语言是Java，若在虚拟机里面运行链码，同样需要在虚拟机里面预先配置Java、JDK、Maven等环境（具体配置参考“1 链下环境”）；

④链码安装方式建议使用**jar包**形式安装，这样比较快一些；关于链码的打包、安装、部署等相关操作教程请参考fabric官网相关说明。
