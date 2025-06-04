pipeline {
    agent any

    stages {
        stage('部署xiaozhi-api') {
            steps {
                echo '部署xiaozhi-api服务'
                sh '''#!/bin/bash
                    # 检查 xiaozhi-api 服务状态并停止
                    if systemctl is-active --quiet xiaozhi-api; then
                        echo "检测到 xiaozhi-api 正在运行，尝试停止..."
                        sudo systemctl stop xiaozhi-api
                        sleep 10 # 等待服务停止
                    fi

                    # 构建并部署 xiaozhi-api
                    cd main/manager-api
                    mvn clean install
                    cd target
                    sudo systemctl start xiaozhi-api

                    # 确认 xiaozhi-api 服务已启动
                    for i in {1..20}; do
                        if sudo systemctl is-active --quiet xiaozhi-api; then
                            echo "xiaozhi-api 启动成功！"
                            break
                        else
                            echo "等待 xiaozhi-api 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    if ! sudo systemctl is-active --quiet xiaozhi-api; then
                        echo "ERROR: xiaozhi-api 启动失败！"
                        exit 1
                    fi
                '''
            }
        }

        stage('部署manager-web') {
            steps {
                echo '部署manager-web服务'
                sh '''#!/bin/bash
                    # 检查 xiaozhi-web 服务状态并停止
                    if systemctl is-active --quiet xiaozhi-web; then
                        echo "检测到 xiaozhi-web 正在运行，尝试停止..."
                        sudo systemctl stop xiaozhi-web
                        sleep 5 # 等待服务停止
                    fi

                    # 构建并部署 manager-web
                    cd main/manager-web
                    npm install
                    sudo systemctl start xiaozhi-web

                    # 确认 manager-web 服务已启动
                    for i in {1..20}; do
                        if sudo systemctl is-active --quiet xiaozhi-web; then
                            echo "manager-web 启动成功！"
                            break
                        else
                            echo "等待 manager-web 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    if ! sudo systemctl is-active --quiet xiaozhi-web; then
                        echo "ERROR: manager-web 启动失败！"
                        exit 1
                    fi
                '''
            }
        }

        stage('部署xiaozhi-server') {
            steps {
                echo '部署xiaozhi-server服务'
                sh '''#!/bin/bash
                    # 检查 xiaozhi-server 服务状态并停止
                    if systemctl is-active --quiet xiaozhi-server; then
                        echo "检测到 xiaozhi-server 正在运行，尝试停止..."
                        sudo systemctl stop xiaozhi-server
                        sleep 10 # 等待服务停止
                    fi

                    # 启动 xiaozhi-server 服务
                    cd main/xiaozhi-server
                    pip install -r requirements.txt
                    sudo systemctl start xiaozhi-server
                    sleep 20

                    # 确认 xiaozhi-server 服务已启动
                    for i in {1..20}; do
                        if sudo systemctl is-active --quiet xiaozhi-server; then
                            echo "xiaozhi-server 启动成功！"
                            break
                        else
                            echo "等待 xiaozhi-server 启动中...（$i）"
                            sleep 5
                        fi
                    done

                    if ! sudo systemctl is-active --quiet xiaozhi-server; then
                        echo "ERROR: xiaozhi-server 启动失败！"
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo '所有服务部署成功！'
        }
        failure {
            echo '部署过程中出现错误！'
        }
    }
}